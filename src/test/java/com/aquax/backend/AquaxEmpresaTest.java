package com.aquax.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
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
class AquaxEmpresaTest {
    private static final java.nio.file.Path AUDITORIA_DB = java.nio.file.Path.of(
            "target", "auditoria-" + java.util.UUID.randomUUID() + ".db").toAbsolutePath();

    @org.springframework.test.context.DynamicPropertySource
    static void configurarAuditoria(org.springframework.test.context.DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> "jdbc:sqlite:" + AUDITORIA_DB);
    }

    private static final String RUTA = "/api/admin/empresa";
    private static final String ADMIN = basic("personal:1234");
    private static final String VENDEDOR = basic("vendedor1:clave-de-vendedor");
    private static final String ACTUALIZACION = """
            {
              "id":999,
              "razonSocial":" AQUAX Actualizada ",
              "ruc":"10715736689",
              "direccion":" Avenida de prueba 123 ",
              "telefono1":"999888777",
              "telefono2":"+51 987654321",
              "correo":"contacto@example.com",
              "registroSanitario":"Registro de prueba",
              "slogan":"Eslogan de prueba",
              "ultimaActualizacion":"1900-01-01"
            }
            """;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void elAdministradorActualizaUnaSolaEmpresaConValidacion() throws Exception {
        mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        mvc.perform(put(RUTA).contentType(MediaType.APPLICATION_JSON).content(ACTUALIZACION))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, basic("personal:incorrecta")))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombreUsuario":"vendedor1","nombre":"Ana","apellido":"Perez",
                         "correo":"vendedor@example.com","contrasena":"clave-de-vendedor","rol":"VENDEDOR"}
                        """))
                .andExpect(status().isCreated());
        mvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, VENDEDOR))
                .andExpect(status().isForbidden());
        mvc.perform(put(RUTA).header(HttpHeaders.AUTHORIZATION, VENDEDOR)
                        .contentType(MediaType.APPLICATION_JSON).content(ACTUALIZACION))
                .andExpect(status().isForbidden());

        consultar().andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.razonSocial").value("AQUAX"))
                .andExpect(jsonPath("$.ruc").value("10715736689"));

        actualizar(ACTUALIZACION).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.razonSocial").value("AQUAX Actualizada"))
                .andExpect(jsonPath("$.direccion").value("Avenida de prueba 123"))
                .andExpect(jsonPath("$.telefono1").value("999888777"))
                .andExpect(jsonPath("$.telefono2").value("+51 987654321"))
                .andExpect(jsonPath("$.correo").value("contacto@example.com"))
                .andExpect(jsonPath("$.registroSanitario").value("Registro de prueba"))
                .andExpect(jsonPath("$.slogan").value("Eslogan de prueba"))
                .andExpect(jsonPath("$.ultimaActualizacion")
                        .value(LocalDate.now(ZoneId.of("America/Lima")).toString()));
        consultar().andExpect(status().isOk())
                .andExpect(jsonPath("$.razonSocial").value("AQUAX Actualizada"))
                .andExpect(jsonPath("$.correo").value("contacto@example.com"));
        assertThat(jdbc.queryForObject(
                "select razon_social from ajustes_empresariales where id=1", String.class))
                .isEqualTo("AQUAX Actualizada");
        assertThat(jdbc.queryForObject("select count(*) from ajustes_empresariales", Long.class)).isEqualTo(1);

        actualizar(ACTUALIZACION).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from ajustes_empresariales", Long.class)).isEqualTo(1);

        actualizar(ACTUALIZACION.replace("10715736689", "123")).andExpect(status().isBadRequest());
        actualizar(ACTUALIZACION.replace("10715736689", "ABCDEFGHIJK")).andExpect(status().isBadRequest());
        actualizar(ACTUALIZACION.replace(" AQUAX Actualizada ", " ")).andExpect(status().isBadRequest());
        actualizar(ACTUALIZACION.replace("contacto@example.com", "invalido")).andExpect(status().isBadRequest());
        actualizar(ACTUALIZACION.replace("999888777", "1".repeat(16))).andExpect(status().isBadRequest());
        actualizar(ACTUALIZACION.replace(" AQUAX Actualizada ", "X".repeat(151))).andExpect(status().isBadRequest());
        consultar().andExpect(status().isOk())
                .andExpect(jsonPath("$.razonSocial").value("AQUAX Actualizada"))
                .andExpect(jsonPath("$.telefono1").value("999888777"))
                .andExpect(jsonPath("$.correo").value("contacto@example.com"));

        actualizar("""
                {"razonSocial":"AQUAX","ruc":"10715736689","direccion":"Direccion minima",
                 "telefono1":null,"telefono2":null,"correo":null,"registroSanitario":null,"slogan":null}
                """).andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono1").doesNotExist())
                .andExpect(jsonPath("$.telefono2").doesNotExist())
                .andExpect(jsonPath("$.correo").doesNotExist())
                .andExpect(jsonPath("$.registroSanitario").doesNotExist())
                .andExpect(jsonPath("$.slogan").doesNotExist());
        consultar().andExpect(status().isOk())
                .andExpect(jsonPath("$.direccion").value("Direccion minima"))
                .andExpect(jsonPath("$.correo").doesNotExist());

        mvc.perform(get("/api/productos")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content("""
                {"nombreCliente":"Cliente","telefonoCliente":"999888777","direccionEntrega":"Direccion",
                 "detalles":[{"productoId":1,"cantidad":2}]}
                """)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.origen").value("CLIENTE"))
                .andExpect(jsonPath("$.total").value(20));

        jdbc.update("delete from ajustes_empresariales where id=1");
        consultar().andExpect(status().isNotFound());
        actualizar(ACTUALIZACION).andExpect(status().isNotFound());
    }

    private ResultActions consultar() throws Exception {
        return mvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, ADMIN));
    }

    private ResultActions actualizar(String json) throws Exception {
        return mvc.perform(put(RUTA).header(HttpHeaders.AUTHORIZATION, ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String basic(String credenciales) {
        return "Basic " + Base64.getEncoder().encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));
    }
}


