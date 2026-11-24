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
class AquaxUsuariosTest {
    private static final java.nio.file.Path AUDITORIA_DB = java.nio.file.Path.of(
            "target", "auditoria-" + java.util.UUID.randomUUID() + ".db").toAbsolutePath();

    @org.springframework.test.context.DynamicPropertySource
    static void configurarAuditoria(org.springframework.test.context.DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> "jdbc:sqlite:" + AUDITORIA_DB);
    }

    private static final String ADMIN = basic("personal:1234");
    private static final String VENDEDOR = basic("vendedor1:clave-de-vendedor");
    private static final String SEGUNDO_ADMIN = basic("admin2:clave-de-vendedor");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void usuariosRolesYAuditoriaFuncionanConElFlujoPublico() throws Exception {
        assertThat(jdbc.queryForObject("select count(*) from usuarios", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select contrasena from usuarios", String.class))
                .startsWith("$2").isNotEqualTo("1234");
        mvc.perform(get("/api/productos")).andExpect(status().isOk());
        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content("""
                {
                  "nombreCliente":"Cliente de prueba",
                  "telefonoCliente":"999888777",
                  "direccionEntrega":"Direccion de prueba",
                  "detalles":[{"productoId":1,"cantidad":2},{"productoId":2,"cantidad":1}]
                }
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(32));
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreUsuario").value("personal"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.contrasena").doesNotExist());
        crearUsuario("Vendedor1", "VENDEDOR", "VENDEDOR@AQUAX.LOCAL")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreUsuario").value("vendedor1"))
                .andExpect(jsonPath("$.correo").value("vendedor@aquax.local"))
                .andExpect(jsonPath("$.contrasena").doesNotExist());
        long vendedorId = jdbc.queryForObject(
                "select id from usuarios where nombre_usuario = 'vendedor1'", Long.class);
        crearUsuario("VENDEDOR1", "VENDEDOR", "otra@aquax.local").andExpect(status().isConflict());
        crearUsuario("otro", "VENDEDOR", "vendedor@aquax.local").andExpect(status().isConflict());
        crearUsuario("incorrecto", "ROL_INEXISTENTE", "incorrecto@aquax.local")
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, VENDEDOR))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("VENDEDOR"));
        mvc.perform(get("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, VENDEDOR))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, VENDEDOR)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/personal/pedidos").header(HttpHeaders.AUTHORIZATION, VENDEDOR))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(patch("/api/personal/pedidos/1/entrega")
                        .header(HttpHeaders.AUTHORIZATION, VENDEDOR)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estadoEntrega\":\"ENTREGADO\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/personal/pedidos/1/pago").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estadoPago\":\"PAGADO\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject(
                "select usuario_id from historial_modificaciones where campo = 'estadoEntrega'", Long.class))
                .isEqualTo(vendedorId);
        assertThat(jdbc.queryForObject(
                "select usuario from historial_modificaciones where campo = 'estadoEntrega'", String.class))
                .isEqualTo("vendedor1");
        mvc.perform(get("/api/personal/pedidos").param("completados", "true")
                        .header(HttpHeaders.AUTHORIZATION, VENDEDOR))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        cambiarActivo(vendedorId, false, ADMIN).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, VENDEDOR))
                .andExpect(status().isUnauthorized());
        cambiarActivo(vendedorId, true, ADMIN).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, VENDEDOR))
                .andExpect(status().isOk());

        long adminId = jdbc.queryForObject(
                "select id from usuarios where nombre_usuario = 'personal'", Long.class);
        cambiarActivo(adminId, false, ADMIN).andExpect(status().isConflict());
        crearUsuario("admin2", "ADMINISTRADOR", "admin2@aquax.local").andExpect(status().isCreated());
        cambiarActivo(adminId, false, SEGUNDO_ADMIN).andExpect(status().isOk());
        long segundoAdminId = jdbc.queryForObject(
                "select id from usuarios where nombre_usuario = 'admin2'", Long.class);
        cambiarActivo(segundoAdminId, false, SEGUNDO_ADMIN).andExpect(status().isConflict());
        mvc.perform(get("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, SEGUNDO_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].contrasena").doesNotExist());
    }

    private ResultActions crearUsuario(String username, String rol, String correo) throws Exception {
        return mvc.perform(post("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombreUsuario":"%s","nombre":"Ana","apellido":"Prueba",
                        "correo":"%s","contrasena":"clave-de-vendedor","rol":"%s"}
                        """.formatted(username, correo, rol)));
    }

    private ResultActions cambiarActivo(long id, boolean activo, String credenciales) throws Exception {
        return mvc.perform(patch("/api/admin/usuarios/" + id + "/activo")
                .header(HttpHeaders.AUTHORIZATION, credenciales).contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":" + activo + "}"));
    }

    private static String basic(String credenciales) {
        return "Basic " + Base64.getEncoder().encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));
    }
}


