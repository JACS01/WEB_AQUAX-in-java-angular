package com.aquax.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {"aquax.personal.usuario=personal", "aquax.personal.contrasena=1234", "aquax.jwt.secret-base64=", "aquax.jwt.expiration-seconds=3600", "aquax.cors.allowed-origins=http://localhost:4200"})
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("sqlite")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class AquaxHistorialTest {
    private static final java.nio.file.Path AUDITORIA_DB = java.nio.file.Path.of(
            "target", "auditoria-" + java.util.UUID.randomUUID() + ".db").toAbsolutePath();

    @org.springframework.test.context.DynamicPropertySource
    static void configurarAuditoria(org.springframework.test.context.DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> "jdbc:sqlite:" + AUDITORIA_DB);
    }

    private static final String INTERNO = "/api/personal/pedidos";
    private static final String ADMIN = basic("personal:1234");
    private static final String VENDEDOR = basic("vendedor1:clave-de-vendedor");
    private static final String PEDIDO = """
            {"nombreCliente":"Cliente de prueba","telefonoCliente":"999888777",
             "direccionEntrega":"Direccion de prueba",
             "detalles":[{"productoId":1,"cantidad":2},{"productoId":2,"cantidad":1}]}
            """;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void historialPorPedidoConPermisosActoresOrdenYCompatibilidad() throws Exception {
        mvc.perform(post("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombreUsuario":"vendedor1","nombre":"Ana","apellido":"Prueba",
                                 "correo":"vendedor@example.com","contrasena":"clave-de-vendedor",
                                 "rol":"VENDEDOR"}
                                """))
                .andExpect(status().isCreated());
        long adminId = idUsuario("personal");
        long vendedorId = idUsuario("vendedor1");
        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                .andExpect(status().isCreated());
        long publico = jdbc.queryForObject("select max(id) from pedidos", Long.class);
        mvc.perform(get(historial(publico))).andExpect(status().isUnauthorized());
        mvc.perform(get(historial(publico)).header(HttpHeaders.AUTHORIZATION, basic("personal:incorrecta")))
                .andExpect(status().isUnauthorized());
        consultar(publico, ADMIN).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        consultar(99999, ADMIN).andExpect(status().isNotFound());

        mvc.perform(post(INTERNO).header(HttpHeaders.AUTHORIZATION, VENDEDOR)
                        .contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                .andExpect(status().isCreated());
        long interno = jdbc.queryForObject("select max(id) from pedidos", Long.class);
        consultar(interno, ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].pedidoId").value(interno))
                .andExpect(jsonPath("$[0].usuarioId").value(vendedorId))
                .andExpect(jsonPath("$[0].usuario").value("vendedor1"))
                .andExpect(jsonPath("$[0].campo").value("creacion"))
                .andExpect(jsonPath("$[0].valorAnterior").value("N/A"))
                .andExpect(jsonPath("$[0].valorNuevo").value("TRABAJADOR"))
                .andExpect(jsonPath("$[0].fecha", endsWith("-05:00")));

        actualizar(interno, "/entrega", "{\"estadoEntrega\":\"EN_CAMINO\"}", ADMIN)
                .andExpect(status().isOk());
        actualizar(interno, "/pago", "{\"estadoPago\":\"PAGADO\"}", VENDEDOR)
                .andExpect(status().isOk());
        actualizar(interno, "/entrega", "{\"estadoEntrega\":\"EN_CAMINO\"}", ADMIN)
                .andExpect(status().isOk());
        actualizar(interno, "/pago", "{\"estadoPago\":\"PAGADO\"}", VENDEDOR)
                .andExpect(status().isOk());
        jdbc.update("update historial_modificaciones set fecha = ? where pedido_id = ?",
                java.sql.Timestamp.from(OffsetDateTime.parse("2026-11-10T14:00:00Z").toInstant()), interno);
        String response = consultar(interno, VENDEDOR).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].campo").value("creacion"))
                .andExpect(jsonPath("$[1].campo").value("estadoEntrega"))
                .andExpect(jsonPath("$[1].usuarioId").value(adminId))
                .andExpect(jsonPath("$[1].usuario").value("personal"))
                .andExpect(jsonPath("$[1].valorAnterior").value("NUEVO"))
                .andExpect(jsonPath("$[1].valorNuevo").value("EN_CAMINO"))
                .andExpect(jsonPath("$[2].campo").value("estadoPago"))
                .andExpect(jsonPath("$[2].usuarioId").value(vendedorId))
                .andExpect(jsonPath("$[2].usuario").value("vendedor1"))
                .andExpect(jsonPath("$[2].valorAnterior").value("PENDIENTE"))
                .andExpect(jsonPath("$[2].valorNuevo").value("PAGADO"))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("contrasena", "clave-de-vendedor", "correo", "$2a$");
        consultar(publico, ADMIN).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        jdbc.update("""
                insert into historial_modificaciones
                (pedido_id, usuario, usuario_id, campo, valor_anterior, valor_nuevo, fecha)
                values (?, 'usuario-anterior', null, 'estadoPago', 'PENDIENTE', 'PAGADO', ?)
                """, publico, java.sql.Timestamp.from(OffsetDateTime.parse("2026-11-10T13:00:00Z").toInstant()));
        consultar(publico, VENDEDOR).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].pedidoId").value(publico))
                .andExpect(jsonPath("$[0].usuario").value("usuario-anterior"))
                .andExpect(jsonPath("$[0].usuarioId").doesNotExist());

        mvc.perform(patch("/api/admin/usuarios/" + vendedorId + "/activo")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk());
        consultar(interno, VENDEDOR).andExpect(status().isUnauthorized());
        consultar(interno, ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuario").value("vendedor1"));
    }

    private long idUsuario(String usuario) {
        return jdbc.queryForObject("select id from usuarios where nombre_usuario = ?", Long.class, usuario);
    }

    private ResultActions consultar(long id, String credenciales) throws Exception {
        return mvc.perform(get(historial(id)).header(HttpHeaders.AUTHORIZATION, credenciales));
    }

    private String historial(long id) {
        return INTERNO + "/" + id + "/historial";
    }

    private ResultActions actualizar(long id, String ruta, String json, String credenciales) throws Exception {
        return mvc.perform(patch(INTERNO + "/" + id + ruta).header(HttpHeaders.AUTHORIZATION, credenciales)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String basic(String credenciales) {
        return "Basic " + Base64.getEncoder().encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));
    }
}


