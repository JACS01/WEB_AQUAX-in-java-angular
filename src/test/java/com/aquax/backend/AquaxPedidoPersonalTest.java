package com.aquax.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
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
class AquaxPedidoPersonalTest {
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
    void registroPublicoEInternoConservaOrigenCreadorYAuditoria() throws Exception {
        long adminId = jdbc.queryForObject(
                "select id from usuarios where nombre_usuario = 'personal'", Long.class);
        mvc.perform(post("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombreUsuario":"vendedor1","nombre":"Ana","apellido":"Prueba",
                                 "correo":"vendedor@example.com","contrasena":"clave-de-vendedor",
                                 "rol":"VENDEDOR"}
                                """))
                .andExpect(status().isCreated());
        long vendedorId = jdbc.queryForObject(
                "select id from usuarios where nombre_usuario = 'vendedor1'", Long.class);

        mvc.perform(post(INTERNO).contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                .andExpect(status().isUnauthorized());
        assertTotals(0, 0, 0);

        mvc.perform(post("/api/pedidos").header(HttpHeaders.AUTHORIZATION, VENDEDOR)
                        .contentType(MediaType.APPLICATION_JSON).content(PEDIDO.replace(
                                "\"nombreCliente\"", "\"origen\":\"TRABAJADOR\",\"usuarioRegistroId\":1,\"usuarioRegistro\":\"personal\",\"nombreCliente\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origen").value("CLIENTE"))
                .andExpect(jsonPath("$.usuarioRegistroId").doesNotExist())
                .andExpect(jsonPath("$.usuarioRegistro").doesNotExist())
                .andExpect(jsonPath("$.total").value(32));
        assertThat(jdbc.queryForObject(
                "select count(*) from pedidos where origen = 'CLIENTE' and usuario_registro_id is null", Long.class))
                .isEqualTo(1);
        assertTotals(1, 2, 0);

        registrar(ADMIN, PEDIDO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origen").value("TRABAJADOR"))
                .andExpect(jsonPath("$.usuarioRegistroId").value(adminId))
                .andExpect(jsonPath("$.usuarioRegistro").value("personal"))
                .andExpect(jsonPath("$.estadoEntrega").value("NUEVO"))
                .andExpect(jsonPath("$.estadoPago").value("PENDIENTE"))
                .andExpect(jsonPath("$.total").value(32))
                .andExpect(jsonPath("$.cantidadProducto").value(3));
        long pedidoAdmin = jdbc.queryForObject(
                "select max(id) from pedidos", Long.class);
        assertThat(jdbc.queryForObject(
                "select usuario_registro_id from pedidos where id = ?", Long.class, pedidoAdmin))
                .isEqualTo(adminId);

        registrar(VENDEDOR, PEDIDO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origen").value("TRABAJADOR"))
                .andExpect(jsonPath("$.usuarioRegistroId").value(vendedorId))
                .andExpect(jsonPath("$.usuarioRegistro").value("vendedor1"));
        long pedidoVendedor = jdbc.queryForObject("select max(id) from pedidos", Long.class);
        assertTotals(3, 6, 2);
        assertThat(jdbc.queryForObject("""
                select count(*) from historial_modificaciones where pedido_id = ? and
                usuario_id = ? and usuario = 'vendedor1' and campo = 'creacion'
                and valor_anterior = 'N/A' and valor_nuevo = 'TRABAJADOR' and fecha is not null
                """, Long.class, pedidoVendedor, vendedorId)).isEqualTo(1);

        registrar(VENDEDOR, PEDIDO.replace("\"cantidad\":1", "\"cantidad\":0"))
                .andExpect(status().isBadRequest());
        registrar(VENDEDOR, PEDIDO.replace(
                "[{\"productoId\":1,\"cantidad\":2},{\"productoId\":2,\"cantidad\":1}]", "null"))
                .andExpect(status().isBadRequest());
        registrar(VENDEDOR, PEDIDO.replace("\"productoId\":2", "\"productoId\":99999"))
                .andExpect(status().isBadRequest());
        assertTotals(3, 6, 2);

        mvc.perform(get(INTERNO).header(HttpHeaders.AUTHORIZATION, ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(pedidoVendedor))
                .andExpect(jsonPath("$[0].usuarioRegistro").value("vendedor1"));
        mvc.perform(patch(INTERNO + "/" + pedidoVendedor + "/entrega")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estadoEntrega\":\"ENTREGADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioRegistroId").value(vendedorId))
                .andExpect(jsonPath("$.usuarioRegistro").value("vendedor1"));
        mvc.perform(patch(INTERNO + "/" + pedidoVendedor + "/pago")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estadoPago\":\"PAGADO\"}"))
                .andExpect(status().isOk());
        mvc.perform(get(INTERNO).param("completados", "true").header(HttpHeaders.AUTHORIZATION, VENDEDOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].usuarioRegistroId").value(vendedorId))
                .andExpect(jsonPath("$[0].usuarioRegistro").value("vendedor1"));
        assertThat(jdbc.queryForObject("""
                select count(*) from historial_modificaciones where pedido_id = ?
                and campo in ('estadoEntrega', 'estadoPago') and usuario_id = ? and usuario = 'personal'
                """, Long.class, pedidoVendedor, adminId)).isEqualTo(2);

        mvc.perform(patch("/api/admin/usuarios/" + vendedorId + "/activo")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk());
        registrar(VENDEDOR, PEDIDO).andExpect(status().isUnauthorized());
        assertTotals(3, 6, 4);
    }

    private ResultActions registrar(String credenciales, String cuerpo) throws Exception {
        return mvc.perform(post(INTERNO).header(HttpHeaders.AUTHORIZATION, credenciales)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private void assertTotals(long pedidos, long detalles, long auditorias) {
        assertThat(jdbc.queryForObject("select count(*) from pedidos", Long.class)).isEqualTo(pedidos);
        assertThat(jdbc.queryForObject("select count(*) from detalles_pedido", Long.class)).isEqualTo(detalles);
        assertThat(jdbc.queryForObject("select count(*) from historial_modificaciones", Long.class)).isEqualTo(auditorias);
    }

    private static String basic(String credenciales) {
        return "Basic " + Base64.getEncoder().encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));
    }
}


