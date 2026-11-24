package com.aquax.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import com.aquax.backend.config.ErroresSeguridad;
import com.aquax.backend.config.JwtAuthenticationFilter;
import com.aquax.backend.config.ProblemasApi;
import com.aquax.backend.repository.UsuarioRepository;
import com.aquax.backend.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {
        "aquax.personal.usuario=seg_admin", "aquax.personal.contrasena=PruebaSeguridad2026!",
        "aquax.jwt.expiration-seconds=3600", "aquax.cors.allowed-origins=http://localhost:4200"})
@AutoConfigureMockMvc
@ActiveProfiles("sqlite")
class AquaxErroresSeguridadTest {

    private static final Path DB = Path.of("target", "errores-seguridad-" + UUID.randomUUID() + ".db").toAbsolutePath();
    private static final String ORIGEN = "http://localhost:4200";
    private static final String ADMIN = basic("seg_admin:PruebaSeguridad2026!");
    private static final String VENDEDOR = basic("seg_vendedor:PruebaSeguridad2026!");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtService jwtService;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProblemasApi problemas;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> "jdbc:sqlite:" + DB);
        registro.add("aquax.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(
                "clave-de-prueba-para-seguridad-2026".getBytes(StandardCharsets.UTF_8)));
    }

    @BeforeEach
    void prepararVendedor() {
        jdbc.update("""
                insert or ignore into usuarios(nombre_usuario,nombre,apellido,correo,contrasena,rol,activo)
                select 'seg_vendedor', 'Ana', 'Prueba', 'seg.vendedor@example.com', contrasena, 'VENDEDOR', 1
                from usuarios where nombre_usuario='seg_admin'
                """);
    }

    @Test
    void preflightPermiteHeadersYMetodoSinAutenticacion() throws Exception {
        mvc.perform(options("/api/personal/pedidos/1/pago")
                        .header(HttpHeaders.ORIGIN, ORIGEN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        org.hamcrest.Matchers.containsString("PATCH")))
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    void origenAjenoEsRechazado() throws Exception {
        mvc.perform(options("/api/personal/pedidos")
                        .header(HttpHeaders.ORIGIN, "https://origen-ajeno.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void credencialesAusentesBasicIncorrectoYBearerInvalidoCompartenContrato() throws Exception {
        String ruta = "/api/personal/pedidos";
        var sinCredenciales = problema(mvc.perform(get(ruta).header(HttpHeaders.ORIGIN, ORIGEN)),
                401, "No autorizado", ruta).andReturn().getResponse();
        assertThat(sinCredenciales.getHeaders(HttpHeaders.WWW_AUTHENTICATE))
                .contains("Bearer", "Basic realm=\"AquaX\"");

        problema(mvc.perform(get(ruta).header(HttpHeaders.ORIGIN, ORIGEN)
                        .header(HttpHeaders.AUTHORIZATION, basic("seg_admin:incorrecta-privada"))),
                401, "No autorizado", ruta)
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"AquaX\""))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("incorrecta-privada"))));

        problema(mvc.perform(get(ruta).header(HttpHeaders.ORIGIN, ORIGEN)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido-privado")),
                401, "No autorizado", ruta)
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("token-invalido-privado"))));

        problema(mvc.perform(get(ruta).header(HttpHeaders.ORIGIN, ORIGEN)
                        .header("X-Requested-With", "XMLHttpRequest")), 401, "No autorizado", ruta);
    }

    @Test
    void rolInsuficienteConBasicYBearerDevuelve403LegibleConCors() throws Exception {
        String ruta = "/api/admin/usuarios";
        problema(mvc.perform(get(ruta).header(HttpHeaders.ORIGIN, ORIGEN)
                        .header(HttpHeaders.AUTHORIZATION, VENDEDOR)),
                403, "Acceso denegado", ruta)
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
        String token = login("seg_vendedor");
        problema(mvc.perform(get(ruta).header(HttpHeaders.ORIGIN, ORIGEN)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)),
                403, "Acceso denegado", ruta)
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    void autenticacionValidaYDescargaConservanAccesoYCabeceras() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .header(HttpHeaders.ORIGIN, ORIGEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreUsuario").value("seg_admin"));
        String token = login("seg_admin");
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(HttpHeaders.ORIGIN, ORIGEN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEN))
                .andExpect(jsonPath("$.nombreUsuario").value("seg_admin"));
        mvc.perform(get("/api/personal/reportes/completados/csv")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(HttpHeaders.ORIGIN, ORIGEN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Disposition"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString(".csv")));
    }

    @Test
    void validacionMuestraCamposYConservaCors() throws Exception {
        problema(mvc.perform(post("/api/pedidos").header(HttpHeaders.ORIGIN, ORIGEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{}")),
                400, "Datos de solicitud inválidos", "/api/pedidos")
                .andExpect(jsonPath("$.errores").isArray())
                .andExpect(jsonPath("$.errores[0].campo").isString())
                .andExpect(jsonPath("$.errores[0].mensaje").isString());
    }

    @Test
    void falloInternoDeAutenticacionEs500SinExponerDatosNiPresentarloComoCredencialesInvalidas() throws Exception {
        UsuarioRepository indisponible = (UsuarioRepository) Proxy.newProxyInstance(
                UsuarioRepository.class.getClassLoader(), new Class<?>[] {UsuarioRepository.class},
                (proxy, metodo, argumentos) -> {
                    throw new DataAccessResourceFailureException("dato-privado-no-publicar");
                });
        String token = jwtService.generarToken(usuarios.findByNombreUsuario("seg_admin").orElseThrow());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/personal/pedidos");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();
        new JwtAuthenticationFilter(jwtService, indisponible, problemas).doFilter(request, response, cadena);
        comprobar500Seguro(response);
        assertThat(cadena.getRequest()).isNull();

        MockHttpServletResponse respuestaBasic = new MockHttpServletResponse();
        new ErroresSeguridad(problemas).commence(request, respuestaBasic,
                new InternalAuthenticationServiceException("dato-privado-no-publicar"));
        comprobar500Seguro(respuestaBasic);
    }

    private ResultActions problema(ResultActions resultado, int estado, String titulo, String ruta) throws Exception {
        return resultado.andExpect(status().is(estado))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(estado))
                .andExpect(jsonPath("$.title").value(titulo))
                .andExpect(jsonPath("$.detail").isString())
                .andExpect(jsonPath("$.instance").value(ruta))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEN));
    }

    private String login(String usuario) throws Exception {
        String respuesta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + usuario + "\",\"contrasena\":\"PruebaSeguridad2026!\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private void comprobar500Seguro(MockHttpServletResponse respuesta) throws Exception {
        assertThat(respuesta.getStatus()).isEqualTo(500);
        assertThat(respuesta.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isNull();
        assertThat(respuesta.getContentAsString())
                .contains("Error interno del servidor")
                .doesNotContain("dato-privado-no-publicar", "DataAccessResourceFailureException");
        assertThat((String) JsonPath.read(respuesta.getContentAsString(), "$.instance"))
                .isEqualTo("/api/personal/pedidos");
    }

    private static String basic(String credenciales) {
        return "Basic " + Base64.getEncoder().encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));
    }
}
