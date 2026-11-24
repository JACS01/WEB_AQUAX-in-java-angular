package com.aquax.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {"aquax.personal.usuario=personal", "aquax.personal.contrasena=1234", "aquax.jwt.secret-base64=", "aquax.jwt.expiration-seconds=3600", "aquax.cors.allowed-origins=http://localhost:4200"})
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("sqlite")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class AquaxPerfilTest {
    private static final java.nio.file.Path AUDITORIA_DB = java.nio.file.Path.of(
            "target", "auditoria-" + java.util.UUID.randomUUID() + ".db").toAbsolutePath();

    @org.springframework.test.context.DynamicPropertySource
    static void configurarAuditoria(org.springframework.test.context.DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> "jdbc:sqlite:" + AUDITORIA_DB);
    }

    private static final String PERFIL = "/api/personal/perfil";
    private static final String ADMIN = basic("personal:1234");
    private static final String VENDEDOR = basic("vendedor1:clave-de-vendedor");
    private static final String PERFIL_ACTUALIZADO = """
            {"id":1,"usuarioId":1,"nombreUsuario":"personal","rol":"ADMINISTRADOR","activo":false,
             "nombre":" Ana Editada ","apellido":" Perez Nueva ","correo":"ANA.NUEVA@example.com",
             "telefono":" 999888777 "}
            """;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;

    @Test
    void cadaCuentaEditaSuPerfilYCambiaSuPropiaContrasena() throws Exception {
        mvc.perform(get(PERFIL)).andExpect(status().isUnauthorized());
        mvc.perform(put(PERFIL).contentType(MediaType.APPLICATION_JSON).content(PERFIL_ACTUALIZADO))
                .andExpect(status().isUnauthorized());
        mvc.perform(patch(PERFIL + "/contrasena").contentType(MediaType.APPLICATION_JSON)
                        .content(cambio("1234", "nueva-clave-admin", "nueva-clave-admin")))
                .andExpect(status().isUnauthorized());

        consultar(ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreUsuario").value("personal"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.telefono").doesNotExist())
                .andExpect(jsonPath("$.contrasena").doesNotExist());
        crearVendedor("vendedor1", "ana@example.com");
        crearVendedor("vendedor2", "otra@example.com");
        String hashInicial = hash("vendedor1");

        consultar(VENDEDOR).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreUsuario").value("vendedor1"))
                .andExpect(jsonPath("$.telefono").doesNotExist());
        actualizar(VENDEDOR, PERFIL_ACTUALIZADO).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreUsuario").value("vendedor1"))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.nombre").value("Ana Editada"))
                .andExpect(jsonPath("$.apellido").value("Perez Nueva"))
                .andExpect(jsonPath("$.correo").value("ana.nueva@example.com"))
                .andExpect(jsonPath("$.telefono").value("999888777"))
                .andExpect(jsonPath("$.rol").value("VENDEDOR"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.contrasena").doesNotExist());
        assertThat(hash("vendedor1")).isEqualTo(hashInicial);
        assertThat(jdbc.queryForObject("select correo from usuarios where nombre_usuario='personal'", String.class))
                .isEqualTo("personal@aquax.local");
        assertThat(jdbc.queryForObject("select correo from usuarios where nombre_usuario='vendedor2'", String.class))
                .isEqualTo("otra@example.com");

        actualizar(VENDEDOR, PERFIL_ACTUALIZADO.replace("ANA.NUEVA@example.com", "PERSONAL@AQUAX.LOCAL"))
                .andExpect(status().isConflict());
        consultar(VENDEDOR).andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("ana.nueva@example.com"));
        actualizar(VENDEDOR, PERFIL_ACTUALIZADO.replace(" Ana Editada ", " "))
                .andExpect(status().isBadRequest());
        actualizar(VENDEDOR, PERFIL_ACTUALIZADO.replace("ANA.NUEVA@example.com", "invalido"))
                .andExpect(status().isBadRequest());
        actualizar(VENDEDOR, PERFIL_ACTUALIZADO.replace(" 999888777 ", "1234567890123456"))
                .andExpect(status().isBadRequest());
        consultar(VENDEDOR).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana Editada"))
                .andExpect(jsonPath("$.telefono").value("999888777"));

        cambiar(VENDEDOR, "incorrecta", "nueva-clave-vendedor", "nueva-clave-vendedor")
                .andExpect(status().isBadRequest());
        cambiar(VENDEDOR, "clave-de-vendedor", "nueva-clave-vendedor", "otra-clave-distinta")
                .andExpect(status().isBadRequest());
        cambiar(VENDEDOR, "clave-de-vendedor", "corta", "corta").andExpect(status().isBadRequest());
        cambiar(VENDEDOR, "clave-de-vendedor", "á".repeat(40), "á".repeat(40))
                .andExpect(status().isBadRequest());
        assertThat(hash("vendedor1")).isEqualTo(hashInicial);

        String respuesta = cambiar(VENDEDOR, "clave-de-vendedor", "nueva-clave-vendedor", "nueva-clave-vendedor")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").isString())
                .andReturn().getResponse().getContentAsString();
        assertThat(respuesta).doesNotContain("nueva-clave-vendedor", "clave-de-vendedor", "$2");
        consultar(VENDEDOR).andExpect(status().isUnauthorized());
        String nuevaAutenticacion = basic("vendedor1:nueva-clave-vendedor");
        consultar(nuevaAutenticacion).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana Editada"))
                .andExpect(jsonPath("$.correo").value("ana.nueva@example.com"));
        assertThat(encoder.matches("nueva-clave-vendedor", hash("vendedor1"))).isTrue();
        actualizar(nuevaAutenticacion, PERFIL_ACTUALIZADO).andExpect(status().isOk());
        consultar(nuevaAutenticacion).andExpect(status().isOk());
        mvc.perform(get("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, nuevaAutenticacion))
                .andExpect(status().isForbidden());

        cambiar(ADMIN, "1234", "nueva-clave-admin", "nueva-clave-admin")
                .andExpect(status().isOk());
        consultar(ADMIN).andExpect(status().isUnauthorized());
        String nuevoAdmin = basic("personal:nueva-clave-admin");
        consultar(nuevoAdmin).andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
        mvc.perform(get("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, nuevoAdmin))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/admin/usuarios/2/activo").header(HttpHeaders.AUTHORIZATION, nuevoAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk());
        consultar(nuevaAutenticacion).andExpect(status().isUnauthorized());
    }

    private void crearVendedor(String nombreUsuario, String correo) throws Exception {
        mvc.perform(post("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombreUsuario":"%s","nombre":"Ana","apellido":"Perez","correo":"%s",
                         "contrasena":"clave-de-vendedor","rol":"VENDEDOR"}
                        """.formatted(nombreUsuario, correo)))
                .andExpect(status().isCreated());
    }

    private ResultActions consultar(String auth) throws Exception {
        return mvc.perform(get(PERFIL).header(HttpHeaders.AUTHORIZATION, auth));
    }

    private ResultActions actualizar(String auth, String json) throws Exception {
        return mvc.perform(put(PERFIL).header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions cambiar(String auth, String actual, String nueva, String confirmar) throws Exception {
        return mvc.perform(patch(PERFIL + "/contrasena").header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON).content(cambio(actual, nueva, confirmar)));
    }

    private String cambio(String actual, String nueva, String confirmar) {
        return """
                {"contrasenaActual":"%s","nuevaContrasena":"%s","confirmarContrasena":"%s"}
                """.formatted(actual, nueva, confirmar);
    }

    private String hash(String usuario) {
        return jdbc.queryForObject("select contrasena from usuarios where nombre_usuario=?", String.class, usuario);
    }

    private static String basic(String credenciales) {
        return "Basic " + Base64.getEncoder().encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));
    }
}


