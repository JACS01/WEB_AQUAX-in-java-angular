package com.aquax.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {"aquax.personal.usuario=personal", "aquax.personal.contrasena=1234", "aquax.jwt.expiration-seconds=3600"})
@org.springframework.test.context.ActiveProfiles("sqlite")
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AquaxTemasCursoTest {

    private static final Path DB = Path.of("target", "curso-" + UUID.randomUUID() + ".db").toAbsolutePath();
    private static final byte[] CLAVE = "0123456789abcdef".repeat(4).getBytes(StandardCharsets.UTF_8);
    private static final String ADMIN = basic("personal:1234");
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> "jdbc:sqlite:" + DB);
        properties.add("aquax.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(CLAVE));
    }

    @Test
    @Order(1)
    void jwtValidaIdentidadPermisosExpiracionYRevocacion() throws Exception {
        String administrador = login("PERSONAL", "1234");
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(administrador)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombreUsuario").value("personal"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"nombreUsuario":"personal","contrasena":"incorrecta"}
                """)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"nombreUsuario":"noexiste","contrasena":"incorrecta"}
                """)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer roto"))
                .andExpect(status().isUnauthorized());
        int inicioFirma = administrador.lastIndexOf('.') + 1;
        String alterado = administrador.substring(0, inicioFirma)
                + (administrador.charAt(inicioFirma) == 'A' ? "B" : "A")
                + administrador.substring(inicioFirma + 1);
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(alterado)))
                .andExpect(status().isUnauthorized());
        String vencido = Jwts.builder().issuer("aquax-backend").subject("personal")
                .claim("uid", 1L).claim("ver", 0L)
                .expiration(Date.from(Instant.now().minusSeconds(1)))
                .signWith(new SecretKeySpec(CLAVE, "HmacSHA256"), Jwts.SIG.HS256).compact();
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(vencido)))
                .andExpect(status().isUnauthorized());
        String otroAlgoritmo = Jwts.builder().issuer("aquax-backend").subject("personal")
                .claim("uid", 1L).claim("ver", 0L)
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(new SecretKeySpec(CLAVE, "HmacSHA384"), Jwts.SIG.HS384).compact();
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(otroAlgoritmo)))
                .andExpect(status().isUnauthorized());

        String empleado = mvc.perform(post("/api/admin/empleados/con-usuario")
                .header(HttpHeaders.AUTHORIZATION, bearer(administrador))
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"usuario":{"nombreUsuario":"jwtvendedor","nombre":"Ana","apellido":"Pérez",
                "correo":"jwt@example.com","contrasena":"Vendedor2026!","rol":"VENDEDOR"},
                "telefono":"999888777","dni":"42424242"}
                """)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = numero(empleado, "$.id");
        String vendedor = login("jwtvendedor", "Vendedor2026!");
        mvc.perform(get("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(vendedor)))
                .andExpect(status().isForbidden());
        actualizarRol(id, "ADMINISTRADOR");
        mvc.perform(get("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(vendedor)))
                .andExpect(status().isOk());
        String promovido = login("jwtvendedor", "Vendedor2026!");
        actualizarRol(id, "VENDEDOR");
        mvc.perform(get("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(promovido)))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/empleados/" + id + "/activo").header(HttpHeaders.AUTHORIZATION, ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/personal/perfil").header(HttpHeaders.AUTHORIZATION, bearer(vendedor)))
                .andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/admin/empleados/" + id + "/activo").header(HttpHeaders.AUTHORIZATION, ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":true}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/personal/perfil/contrasena")
                .header(HttpHeaders.AUTHORIZATION, bearer(vendedor))
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"contrasenaActual":"Vendedor2026!","nuevaContrasena":"VendedorNueva2026!",
                "confirmarContrasena":"VendedorNueva2026!"}
                """)).andExpect(status().isOk());
        mvc.perform(get("/api/personal/perfil").header(HttpHeaders.AUTHORIZATION, bearer(vendedor)))
                .andExpect(status().isUnauthorized());
        String renovado = login("jwtvendedor", "VendedorNueva2026!");
        mvc.perform(get("/api/personal/perfil").header(HttpHeaders.AUTHORIZATION, bearer(renovado)))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject(
                "select version_token from usuarios where nombre_usuario='jwtvendedor'", Long.class)).isEqualTo(1L);
    }

    @Test
    @Order(2)
    void paginasFiltranOrdenanLimitanYConservanListados() throws Exception {
        for (int i = 0; i < 55; i++) {
            jdbc.update("""
                    insert into usuarios(nombre_usuario,nombre,apellido,correo,contrasena,rol,activo)
                    select ?, 'Mismo', 'Apellido', ?, contrasena, 'VENDEDOR', 1
                    from usuarios where nombre_usuario='personal'
                    """, "pagina" + i, "pagina" + i + "@example.com");
        }
        pagina("/api/admin/usuarios/pagina?texto=pagina&size=10&sort=nombre,asc")
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.page.size").value(10)).andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(55))
                .andExpect(jsonPath("$.page.totalPages").value(6))
                .andExpect(jsonPath("$.content[0].contrasena").doesNotExist());
        String primera = pagina("/api/admin/usuarios/pagina?texto=pagina&size=10&sort=nombre,asc")
                .andReturn().getResponse().getContentAsString();
        String segunda = pagina("/api/admin/usuarios/pagina?texto=pagina&size=10&page=1&sort=nombre,asc")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Integer> idsPrimera = JsonPath.read(primera, "$.content[*].id");
        List<Integer> idsSegunda = JsonPath.read(segunda, "$.content[*].id");
        assertThat(idsPrimera).doesNotContainAnyElementsOf(idsSegunda);
        assertThat(idsPrimera).isSorted();
        pagina("/api/admin/usuarios/pagina?size=9999").andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(50)).andExpect(jsonPath("$.content.length()").value(50));
        pagina("/api/admin/usuarios/pagina?page=-1").andExpect(status().isBadRequest());
        pagina("/api/admin/usuarios/pagina?size=0").andExpect(status().isBadRequest());
        pagina("/api/admin/usuarios/pagina?sort=contrasena").andExpect(status().isBadRequest());
        pagina("/api/admin/usuarios/pagina?page=1.5").andExpect(status().isBadRequest());
        pagina("/api/admin/usuarios/pagina?texto=pagina&page=999").andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(55));
        jdbc.update("update usuarios set nombre='ANA PÉREZ' where nombre_usuario='pagina0'");
        mvc.perform(get("/api/admin/usuarios/pagina").param("texto", "pérez")
                .header(HttpHeaders.AUTHORIZATION, ADMIN)).andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2));
        mvc.perform(get("/api/admin/empleados/pagina").param("texto", "pérez")
                .header(HttpHeaders.AUTHORIZATION, ADMIN)).andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1));

        long ultimoId = 0;
        for (int i = 0; i < 5; i++) {
            String respuesta = mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content("""
                    {"nombreCliente":"Curso%%_! %d","telefonoCliente":"999888777","direccionEntrega":"Prueba",
                    "detalles":[{"productoId":1,"cantidad":2},{"productoId":2,"cantidad":1}]}
                    """.formatted(i))).andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            ultimoId = numero(respuesta, "$.id");
        }
        mvc.perform(get("/api/personal/pedidos/pagina").param("texto", "Curso%_!").param("size", "2")
                .header(HttpHeaders.AUTHORIZATION, ADMIN)).andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(5))
                .andExpect(jsonPath("$.page.totalPages").value(3))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].detalles.length()").value(2));
        mvc.perform(get("/api/personal/pedidos/pagina").param("texto", "AQX-%04d".formatted(ultimoId))
                .header(HttpHeaders.AUTHORIZATION, ADMIN)).andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(ultimoId));
        mvc.perform(get("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, ADMIN))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(57));
        mvc.perform(get("/api/personal/pedidos").header(HttpHeaders.AUTHORIZATION, ADMIN))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    @Order(3)
    void swaggerDescribeSeguridadYContratosReales() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        String documento = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.basicAuth.scheme").value("basic"))
                .andReturn().getResponse().getContentAsString();
        assertThat(documento).contains("/api/auth/login", "/api/admin/usuarios/pagina", "PaginaResponse",
                "/api/personal/reportes/completados/csv", "application/problem+json");
        assertThat(documento).doesNotContain(Base64.getEncoder().encodeToString(CLAVE));
        assertThat(documento).doesNotContain("ErrorSeguridad");
        String esquemaNoAutorizado = JsonPath.read(documento,
                "$.components.responses.NoAutorizado.content['application/problem+json'].schema['$ref']");
        String esquemaProhibido = JsonPath.read(documento,
                "$.components.responses.Prohibido.content['application/problem+json'].schema['$ref']");
        assertThat(esquemaNoAutorizado).isEqualTo("#/components/schemas/ProblemaApi");
        assertThat(esquemaProhibido).isEqualTo(esquemaNoAutorizado);
        assertThat((String) JsonPath.read(documento,
                "$.paths['/api/productos'].get.responses['500']['$ref']"))
                .isEqualTo("#/components/responses/ErrorInterno");
        List<?> seguridadLogin = JsonPath.read(documento, "$.paths['/api/auth/login'].post.security");
        assertThat(seguridadLogin).isEmpty();
        List<?> seguridadCatalogo = JsonPath.read(documento, "$.paths['/api/productos'].get.security");
        assertThat(seguridadCatalogo).isEmpty();
    }

    private String login(String nombreUsuario, String contrasena) throws Exception {
        String respuesta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreUsuario\":\"" + nombreUsuario + "\",\"contrasena\":\"" + contrasena + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.usuario.contrasena").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private void actualizarRol(long id, String rol) throws Exception {
        mvc.perform(patch("/api/admin/empleados/" + id + "/rol").header(HttpHeaders.AUTHORIZATION, ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"" + rol + "\"}"))
                .andExpect(status().isOk());
    }

    private ResultActions pagina(String ruta) throws Exception {
        return mvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, ADMIN));
    }

    private static long numero(String json, String ruta) {
        return ((Number) JsonPath.read(json, ruta)).longValue();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String basic(String valor) {
        return "Basic " + Base64.getEncoder().encodeToString(valor.getBytes(StandardCharsets.UTF_8));
    }
}


