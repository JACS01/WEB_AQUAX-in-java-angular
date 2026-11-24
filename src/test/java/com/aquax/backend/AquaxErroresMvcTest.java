package com.aquax.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest(properties = {
        "aquax.personal.usuario=personal", "aquax.personal.contrasena=1234", "aquax.jwt.secret-base64="
})
@ActiveProfiles("sqlite")
@AutoConfigureMockMvc
class AquaxErroresMvcTest {

    private static final Path DB = Path.of("target", "errores-mvc-" + UUID.randomUUID() + ".db").toAbsolutePath();
    private static final String ADMIN = "Basic " + Base64.getEncoder()
            .encodeToString("personal:1234".getBytes(StandardCharsets.UTF_8));

    @Autowired MockMvc mvc;
    @Autowired RequestMappingHandlerMapping rutas;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> "jdbc:sqlite:" + DB);
    }

    @Test
    void validacionConservaCamposSinExponerContrasenas() throws Exception {
        String cuerpo = problema(mvc.perform(patch("/api/personal/perfil/contrasena")
                .header(HttpHeaders.AUTHORIZATION, ADMIN).contentType(MediaType.APPLICATION_JSON).content("""
                {"contrasenaActual":"privada-no-debe-salir","nuevaContrasena":"corta","confirmarContrasena":"corta"}
                """)), 400, "/api/personal/perfil/contrasena")
                .andExpect(jsonPath("$.title").value("Datos de solicitud inválidos"))
                .andExpect(jsonPath("$.errores[0].campo").value("nuevaContrasena"))
                .andExpect(jsonPath("$.errores[0].mensaje").isString())
                .andReturn().getResponse().getContentAsString();
        assertThat(cuerpo).doesNotContain("privada-no-debe-salir", "corta", "rejectedValue");
    }

    @Test
    void jsonIlegibleNoDevuelveSuContenido() throws Exception {
        String cuerpo = problema(mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreCliente\":\"dato-privado-no-debe-salir\",\"detalles\":[")), 400, "/api/pedidos")
                .andExpect(jsonPath("$.title").value("Contenido de solicitud inválido"))
                .andExpect(jsonPath("$.detail").value("El cuerpo debe ser un JSON válido con los tipos de datos esperados."))
                .andReturn().getResponse().getContentAsString();
        assertThat(cuerpo).doesNotContain("dato-privado-no-debe-salir", "JsonParseException");
    }

    @Test
    void erroresDeParametrosSonUtilesYNoRepitenValoresRechazados() throws Exception {
        String pagina = problema(mvc.perform(get("/api/admin/usuarios/pagina")
                .header(HttpHeaders.AUTHORIZATION, ADMIN).param("page", "valor-no-entero")),
                400, "/api/admin/usuarios/pagina")
                .andExpect(jsonPath("$.errores[0].campo").value("page"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("Debe ser un número entero válido."))
                .andReturn().getResponse().getContentAsString();
        assertThat(pagina).doesNotContain("valor-no-entero", "For input string");

        problema(mvc.perform(get("/api/admin/empleados/no-es-numero")
                .header(HttpHeaders.AUTHORIZATION, ADMIN)), 400, "/api/admin/empleados/no-es-numero")
                .andExpect(jsonPath("$.errores[0].campo").value("id"));
        problema(mvc.perform(get("/api/personal/reportes/completados")
                .header(HttpHeaders.AUTHORIZATION, ADMIN).param("desde", "2026-02-30")),
                400, "/api/personal/reportes/completados")
                .andExpect(jsonPath("$.errores[0].campo").value("desde"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("Debe ser una fecha válida con formato yyyy-MM-dd."));
    }

    @Test
    void conservaEstadosDeNegocioConTitulosEspanoles() throws Exception {
        problema(mvc.perform(get("/api/admin/empleados/999999")
                .header(HttpHeaders.AUTHORIZATION, ADMIN)), 404, "/api/admin/empleados/999999")
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.detail").value("No se encontró el empleado."));
        problema(mvc.perform(patch("/api/admin/usuarios/1/activo").header(HttpHeaders.AUTHORIZATION, ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}")),
                409, "/api/admin/usuarios/1/activo")
                .andExpect(jsonPath("$.title").value("Conflicto"));
        problema(mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content("""
                {"nombreCliente":"Prueba","telefonoCliente":"999888777","direccionEntrega":"Prueba",
                 "detalles":[{"productoId":999999,"cantidad":1}]}
                """)), 400, "/api/pedidos")
                .andExpect(jsonPath("$.detail").value("No existe un producto con el identificador 999999."));
    }

    @Test
    void erroresHttpConservanEstadoYHeadersDelProtocolo() throws Exception {
        problema(mvc.perform(post("/api/pedidos").contentType(MediaType.TEXT_PLAIN).content("dato-privado")),
                415, "/api/pedidos")
                .andExpect(jsonPath("$.title").value("Tipo de contenido no admitido"));
        problema(mvc.perform(get("/api/admin/usuarios/1/activo").header(HttpHeaders.AUTHORIZATION, ADMIN)),
                405, "/api/admin/usuarios/1/activo")
                .andExpect(header().string(HttpHeaders.ALLOW, "PATCH"))
                .andExpect(jsonPath("$.title").value("Método no permitido"));
    }

    @Test
    void falloInesperadoDevuelve500SinDetallesInternos() throws Exception {
        String respuesta = ejecutarFallo(new IllegalStateException("SQL privado token-no-debe-salir"), 500)
                .andExpect(jsonPath("$.title").value("Error interno del servidor"))
                .andExpect(jsonPath("$.detail").value("Ocurrió un error interno. Inténtalo nuevamente más tarde."))
                .andReturn().getResponse().getContentAsString();
        assertThat(respuesta).doesNotContain("SQL privado", "token-no-debe-salir", "IllegalStateException", "stackTrace");
    }

    @Test
    void falloDeRestriccionNoExponeSqlNiDatos() throws Exception {
        String respuesta = ejecutarFallo(new DataIntegrityViolationException("INSERT secreto-no-debe-salir"), 409)
                .andExpect(jsonPath("$.title").value("Conflicto"))
                .andReturn().getResponse().getContentAsString();
        assertThat(respuesta).doesNotContain("INSERT", "secreto-no-debe-salir", "DataIntegrityViolationException");
    }

    @Test
    void falloInternoDeAutenticacionNoSeConfundeConCredencialesIncorrectas() throws Exception {
        String respuesta = ejecutarFallo(new AuthenticationServiceException("detalle-de-infraestructura-privado"), 500)
                .andExpect(jsonPath("$.title").value("Error interno del servidor"))
                .andReturn().getResponse().getContentAsString();
        assertThat(respuesta).doesNotContain("detalle-de-infraestructura-privado", "AuthenticationServiceException");
    }

    private ResultActions problema(ResultActions respuesta, int estado, String ruta) throws Exception {
        return respuesta.andExpect(status().is(estado))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(estado))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.instance").value(ruta));
    }

    private ResultActions ejecutarFallo(RuntimeException fallo, int estado) throws Exception {
        String ruta = "/api/personal/fallo-prueba-" + UUID.randomUUID();
        RequestMappingInfo mapping = RequestMappingInfo.paths(ruta).methods(RequestMethod.GET)
                .options(rutas.getBuilderConfiguration()).build();
        rutas.registerMapping(mapping, new FalloDePrueba(fallo), FalloDePrueba.class.getMethod("ejecutar"));
        try {
            return problema(mvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, ADMIN)), estado, ruta);
        } finally {
            rutas.unregisterMapping(mapping);
        }
    }

    public static class FalloDePrueba {
        private final RuntimeException fallo;

        FalloDePrueba(RuntimeException fallo) {
            this.fallo = fallo;
        }

        public String ejecutar() {
            throw fallo;
        }
    }
}
