package com.aquax.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"aquax.personal.usuario=personal", "aquax.personal.contrasena=1234", "aquax.jwt.secret-base64=", "aquax.jwt.expiration-seconds=3600", "aquax.cors.allowed-origins=http://localhost:4200"})
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("sqlite")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class AquaxAuditoriaEntradaTest {

    private static final Path DB = Path.of("target", "entradas-" + UUID.randomUUID() + ".db").toAbsolutePath();
    private static final String ADMIN = "Basic " + Base64.getEncoder().encodeToString(
            "personal:1234".getBytes(StandardCharsets.UTF_8));
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> "jdbc:sqlite:" + DB);
    }

    @Test
    void cantidadFraccionariaSeRechaza() throws Exception {
        var resultado = mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content("""
                {"nombreCliente":"Entrada fraccionaria","telefonoCliente":"999888777","direccionEntrega":"Prueba",
                 "detalles":[{"productoId":1,"cantidad":2.9}]}
                """)).andReturn();
        org.assertj.core.api.Assertions.assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void zoomFraccionarioSeRechaza() throws Exception {
        var resultado = mvc.perform(put("/api/personal/preferencias")
                .header(HttpHeaders.AUTHORIZATION, ADMIN).contentType(MediaType.APPLICATION_JSON).content("""
                {"idioma":"es","nivelZoom":120.9,"filtroColor":"none","filtroColorActivo":false}
                """)).andReturn();
        org.assertj.core.api.Assertions.assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void enumNumericoSeRechaza() throws Exception {
        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content("""
                {"nombreCliente":"Entrada enum","telefonoCliente":"999888777","direccionEntrega":"Prueba",
                 "detalles":[{"productoId":1,"cantidad":1}]}
                """)).andExpect(status().isCreated());
        long pedidoId = jdbc.queryForObject("select max(id) from pedidos", Long.class);
        var resultado = mvc.perform(patch("/api/personal/pedidos/" + pedidoId + "/pago")
                .header(HttpHeaders.AUTHORIZATION, ADMIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"estadoPago\":1}")).andReturn();
        org.assertj.core.api.Assertions.assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void booleanoNumericoSeRechaza() throws Exception {
        mvc.perform(patch("/api/admin/usuarios/1/activo")
                .header(HttpHeaders.AUTHORIZATION, ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void preflightAngularSeAutoriza() throws Exception {
        var resultado = mvc.perform(options("/api/personal/pedidos")
                .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization")).andReturn();
        org.assertj.core.api.Assertions.assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo("http://localhost:4200");
    }

    @Test
    void validacionExplicaCampoSinDevolverContrasenas() throws Exception {
        var resultado = mvc.perform(patch("/api/personal/perfil/contrasena")
                .header(HttpHeaders.AUTHORIZATION, ADMIN).contentType(MediaType.APPLICATION_JSON).content("""
                {"contrasenaActual":"secreto-que-no-debe-salir","nuevaContrasena":"12345","confirmarContrasena":"12345"}
                """)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Datos de solicitud inválidos"))
                .andExpect(jsonPath("$.errores[0].campo").value("nuevaContrasena"))
                .andExpect(jsonPath("$.errores[0].mensaje").isString())
                .andReturn();
        assertThat(resultado.getResponse().getContentAsString())
                .doesNotContain("secreto-que-no-debe-salir", "12345");
    }

    @Test
    void jsonIlegibleDevuelveDetalleEspanolSinDatosDelCuerpo() throws Exception {
        var resultado = mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreCliente\":\"texto-privado\",\"detalles\":["))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El cuerpo debe ser un JSON válido con los tipos de datos esperados."))
                .andReturn();
        assertThat(resultado.getResponse().getContentAsString()).doesNotContain("texto-privado");
    }

    @Test
    void corsMantieneAutenticacionRolesYDescarga() throws Exception {
        mvc.perform(get("/api/personal/pedidos").header(HttpHeaders.ORIGIN, "http://localhost:4200"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
        mvc.perform(post("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"nombreUsuario":"auditoria_vendedor","nombre":"Ana","apellido":"Prueba","correo":"auditoria.vendedor@example.com",
                 "contrasena":"ClaveVendedor2026!","rol":"VENDEDOR"}
                """)).andExpect(status().isCreated());
        String vendedor = "Basic " + Base64.getEncoder().encodeToString(
                "auditoria_vendedor:ClaveVendedor2026!".getBytes(StandardCharsets.UTF_8));
        mvc.perform(get("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, vendedor)
                .header(HttpHeaders.ORIGIN, "http://localhost:4200"))
                .andExpect(status().isForbidden())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
        mvc.perform(get("/api/personal/reportes/completados/csv").header(HttpHeaders.AUTHORIZATION, ADMIN)
                .header(HttpHeaders.ORIGIN, "http://localhost:4200"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Disposition"));
        mvc.perform(get("/api/productos").header(HttpHeaders.ORIGIN, "https://example.com"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}

