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
class AquaxNotificacionesTest {
    private static final java.nio.file.Path AUDITORIA_DB = java.nio.file.Path.of(
            "target", "auditoria-" + java.util.UUID.randomUUID() + ".db").toAbsolutePath();

    @org.springframework.test.context.DynamicPropertySource
    static void configurarAuditoria(org.springframework.test.context.DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> "jdbc:sqlite:" + AUDITORIA_DB);
    }

    private static final String RUTA = "/api/personal/notificaciones";
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
    void notificacionesAtomicasPorCuentaConLecturaAislada() throws Exception {
        crearVendedor("vendedor1");
        crearVendedor("inactivo");
        long adminId = idUsuario("personal");
        long vendedorId = idUsuario("vendedor1");
        long inactivoId = idUsuario("inactivo");
        mvc.perform(patch("/api/admin/usuarios/" + inactivoId + "/activo")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isOk());

        mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        mvc.perform(get(RUTA + "/no-leidas")).andExpect(status().isUnauthorized());
        mvc.perform(patch(RUTA + "/99999/leida")).andExpect(status().isUnauthorized());
        mvc.perform(patch(RUTA + "/leidas")).andExpect(status().isUnauthorized());
        mvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, basic("personal:incorrecta")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, basic("inactivo:clave-de-vendedor")))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/personal/pedidos").header(HttpHeaders.AUTHORIZATION, VENDEDOR)
                        .contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                .andExpect(status().isCreated());
        assertThat(cantidad("notificaciones")).isZero();
        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content(PEDIDO.replace("\"cantidad\":2", "\"cantidad\":0")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content(PEDIDO.replace("\"productoId\":2", "\"productoId\":99999")))
                .andExpect(status().isBadRequest());
        assertThat(cantidad("notificaciones")).isZero();
        assertThat(cantidad("pedidos")).isEqualTo(1);

        crearPedidoPublico();
        long primerPedido = jdbc.queryForObject("select max(id) from pedidos", Long.class);
        long notificacionAdmin = idNotificacion(primerPedido, adminId);
        long notificacionVendedor = idNotificacion(primerPedido, vendedorId);
        assertThat(cantidad("notificaciones")).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "select count(*) from notificaciones where usuario_id = ?", Long.class, inactivoId)).isZero();
        consultar(ADMIN, true).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(notificacionAdmin))
                .andExpect(jsonPath("$[0].pedidoId").value(primerPedido))
                .andExpect(jsonPath("$[0].numeroPedido").value("AQX-%04d".formatted(primerPedido)))
                .andExpect(jsonPath("$[0].tipo").value("pedido_nuevo"))
                .andExpect(jsonPath("$[0].leida").value(false))
                .andExpect(jsonPath("$[0].fechaCreacion", endsWith("-05:00")));
        consultar(VENDEDOR, true).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(notificacionVendedor));
        contar(ADMIN).andExpect(jsonPath("$.cantidad").value(1));
        contar(VENDEDOR).andExpect(jsonPath("$.cantidad").value(1));

        marcar(notificacionAdmin, VENDEDOR).andExpect(status().isNotFound());
        marcar(99999, ADMIN).andExpect(status().isNotFound());
        marcar(notificacionAdmin, ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificacionAdmin))
                .andExpect(jsonPath("$.leida").value(true));
        marcar(notificacionAdmin, ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.leida").value(true));
        consultar(ADMIN, true).andExpect(jsonPath("$.length()").value(0));
        contar(ADMIN).andExpect(jsonPath("$.cantidad").value(0));
        contar(VENDEDOR).andExpect(jsonPath("$.cantidad").value(1));
        consultar(ADMIN, false).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].leida").value(true));

        crearPedidoPublico();
        long segundoPedido = jdbc.queryForObject("select max(id) from pedidos", Long.class);
        jdbc.update("update notificaciones set fecha_creacion = ?",
                java.sql.Timestamp.from(OffsetDateTime.parse("2026-11-11T05:00:00Z").toInstant()));
        String all = consultar(ADMIN, false).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].pedidoId").value(segundoPedido))
                .andExpect(jsonPath("$[1].pedidoId").value(primerPedido))
                .andReturn().getResponse().getContentAsString();
        assertThat(all).doesNotContain("contrasena", "clave-de-vendedor", "correo", "$2a$");
        contar(ADMIN).andExpect(jsonPath("$.cantidad").value(1));
        contar(VENDEDOR).andExpect(jsonPath("$.cantidad").value(2));
        marcarTodas(ADMIN).andExpect(status().isOk()).andExpect(jsonPath("$.actualizadas").value(1));
        marcarTodas(ADMIN).andExpect(status().isOk()).andExpect(jsonPath("$.actualizadas").value(0));
        contar(ADMIN).andExpect(jsonPath("$.cantidad").value(0));
        contar(VENDEDOR).andExpect(jsonPath("$.cantidad").value(2));
        marcarTodas(VENDEDOR).andExpect(status().isOk()).andExpect(jsonPath("$.actualizadas").value(2));
        contar(VENDEDOR).andExpect(jsonPath("$.cantidad").value(0));

        long pedidosAntes = cantidad("pedidos");
        long detallesAntes = cantidad("detalles_pedido");
        long avisosAntes = cantidad("notificaciones");
        jdbc.execute("CREATE TRIGGER fallo_controlado BEFORE INSERT ON notificaciones WHEN NEW.pedido_id > " + segundoPedido + " BEGIN SELECT RAISE(ABORT, 'Fallo controlado'); END");
        try {
            mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.title").value("Error interno del servidor"))
                    .andExpect(jsonPath("$.detail").value("Ocurrió un error interno. Inténtalo nuevamente más tarde."))
                    .andExpect(jsonPath("$.instance").value("/api/pedidos"));
            assertThat(cantidad("pedidos")).isEqualTo(pedidosAntes);
            assertThat(cantidad("detalles_pedido")).isEqualTo(detallesAntes);
            assertThat(cantidad("notificaciones")).isEqualTo(avisosAntes);
        } finally {
            jdbc.execute("DROP TRIGGER fallo_controlado");
        }
    }

    private void crearVendedor(String nombre) throws Exception {
        mvc.perform(post("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombreUsuario":"%s","nombre":"Ana","apellido":"Prueba",
                                 "correo":"%s@example.com","contrasena":"clave-de-vendedor","rol":"VENDEDOR"}
                                """.formatted(nombre, nombre)))
                .andExpect(status().isCreated());
    }

    private void crearPedidoPublico() throws Exception {
        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                .andExpect(status().isCreated());
    }

    private long idUsuario(String nombre) {
        return jdbc.queryForObject("select id from usuarios where nombre_usuario = ?", Long.class, nombre);
    }

    private long idNotificacion(long pedidoId, long usuarioId) {
        return jdbc.queryForObject("select id from notificaciones where pedido_id = ? and usuario_id = ?",
                Long.class, pedidoId, usuarioId);
    }

    private long cantidad(String tabla) {
        return jdbc.queryForObject("select count(*) from " + tabla, Long.class);
    }

    private ResultActions consultar(String auth, boolean soloNoLeidas) throws Exception {
        return mvc.perform(get(RUTA).param("soloNoLeidas", Boolean.toString(soloNoLeidas))
                .header(HttpHeaders.AUTHORIZATION, auth));
    }

    private ResultActions contar(String auth) throws Exception {
        return mvc.perform(get(RUTA + "/no-leidas").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk());
    }

    private ResultActions marcar(long id, String auth) throws Exception {
        return mvc.perform(patch(RUTA + "/" + id + "/leida").header(HttpHeaders.AUTHORIZATION, auth));
    }

    private ResultActions marcarTodas(String auth) throws Exception {
        return mvc.perform(patch(RUTA + "/leidas").header(HttpHeaders.AUTHORIZATION, auth));
    }

    private static String basic(String credenciales) {
        return "Basic " + Base64.getEncoder().encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));
    }
}


