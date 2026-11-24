package com.aquax.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {"aquax.personal.usuario=personal", "aquax.personal.contrasena=1234", "aquax.jwt.secret-base64=", "aquax.jwt.expiration-seconds=3600", "aquax.cors.allowed-origins=http://localhost:4200"})
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("sqlite")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AquaxEmpleadoEdicionTest {

    private static final Path DB = Path.of("target", "empleado-edicion-" + UUID.randomUUID() + ".db").toAbsolutePath();
    private static final String ADMIN = basic("personal:1234");
    private static final String VENDEDOR = basic("vendedor1:ClaveVendedor2026!");
    private static final String PERFIL = """
            {"nombre":" Ana nueva ","apellido":" Pérez nuevo ","correo":"ANA.NUEVA@example.com","telefono":"999777666",
             "usuarioId":1,"id":1,"rol":"ADMINISTRADOR","activo":false}
            """;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> "jdbc:sqlite:" + DB);
    }

    @Test
    @Order(1)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void adminEditaPerfilYRolDelEmpleado() throws Exception {
        enviar("POST", "/api/admin/empleados/con-usuario", ADMIN, """
                {"usuario":{"nombreUsuario":"vendedor1","nombre":"Ana","apellido":"Pérez",
                "correo":"ana@example.com","contrasena":"ClaveVendedor2026!","rol":"VENDEDOR"},
                "telefono":"999888777","dni":"12345678","fechaIngreso":"2026-11-01"}
                """).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.usuarioId").value(2));
        enviar("POST", "/api/admin/empleados", ADMIN, """
                {"usuarioId":1,"dni":"87654321","fechaIngreso":"2026-11-01"}
                """).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(2));

        enviar("PUT", "/api/admin/empleados/1/perfil", null, PERFIL).andExpect(status().isUnauthorized());
        enviar("PATCH", "/api/admin/empleados/1/rol", null, "{\"rol\":\"ADMINISTRADOR\"}")
                .andExpect(status().isUnauthorized());
        enviar("PUT", "/api/admin/empleados/1/perfil", VENDEDOR, PERFIL).andExpect(status().isForbidden());
        enviar("PATCH", "/api/admin/empleados/1/rol", VENDEDOR, "{\"rol\":\"ADMINISTRADOR\"}")
                .andExpect(status().isForbidden());
        enviar("PUT", "/api/admin/empleados/99999/perfil", ADMIN, PERFIL).andExpect(status().isNotFound());
        enviar("PATCH", "/api/admin/empleados/99999/rol", ADMIN, "{\"rol\":\"VENDEDOR\"}")
                .andExpect(status().isNotFound());
        enviar("PATCH", "/api/admin/empleados/1/rol", ADMIN, "{}").andExpect(status().isBadRequest());
        enviar("PATCH", "/api/admin/empleados/1/rol", ADMIN, "{\"rol\":\"OTRO\"}").andExpect(status().isBadRequest());
        enviar("PUT", "/api/admin/empleados/1/perfil", ADMIN,
                "{\"nombre\":\"\",\"apellido\":\"Pérez\",\"correo\":\"ana@example.com\",\"telefono\":\"999888777\"}")
                .andExpect(status().isBadRequest());
        enviar("PUT", "/api/admin/empleados/1/perfil", ADMIN,
                "{\"nombre\":\"No guardar\",\"apellido\":\"Pérez\",\"correo\":\"PERSONAL@aquax.local\",\"telefono\":\"999888777\"}")
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select nombre from usuarios where id=2", String.class)).isEqualTo("Ana");

        String hash = jdbc.queryForObject("select contrasena from usuarios where id=2", String.class);
        enviar("PUT", "/api/admin/empleados/1/perfil", ADMIN, PERFIL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana nueva"))
                .andExpect(jsonPath("$.apellido").value("Pérez nuevo"))
                .andExpect(jsonPath("$.correo").value("ana.nueva@example.com"))
                .andExpect(jsonPath("$.telefono").value("999777666"))
                .andExpect(jsonPath("$.dni").value("12345678"))
                .andExpect(jsonPath("$.fechaIngreso").value("2026-11-01"))
                .andExpect(jsonPath("$.nombreUsuario").value("vendedor1"))
                .andExpect(jsonPath("$.rol").value("VENDEDOR"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.contrasena").doesNotExist());
        assertThat(jdbc.queryForObject("select contrasena from usuarios where id=2", String.class)).isEqualTo(hash);
        enviar("GET", "/api/personal/perfil", VENDEDOR, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.correo").value("ana.nueva@example.com"));
        enviar("GET", "/api/admin/usuarios", ADMIN, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$[1].nombre").value("Ana nueva"));
        assertThat(jdbc.queryForObject("select nombre from usuarios where id=1", String.class)).isEqualTo("Administrador");

        enviar("PATCH", "/api/admin/empleados/2/rol", ADMIN, "{\"rol\":\"VENDEDOR\"}")
                .andExpect(status().isConflict());
        enviar("PATCH", "/api/admin/empleados/1/rol", ADMIN, "{\"rol\":\"ADMINISTRADOR\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
        enviar("PATCH", "/api/admin/empleados/1/rol", ADMIN, "{\"rol\":\"ADMINISTRADOR\"}")
                .andExpect(status().isOk());
        enviar("GET", "/api/admin/usuarios", VENDEDOR, null).andExpect(status().isOk());
        enviar("PATCH", "/api/admin/empleados/1/rol", ADMIN, "{\"rol\":\"VENDEDOR\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("VENDEDOR"));
        enviar("GET", "/api/admin/usuarios", VENDEDOR, null).andExpect(status().isForbidden());
        enviar("GET", "/api/personal/pedidos", VENDEDOR, null).andExpect(status().isOk());
        enviar("PATCH", "/api/admin/empleados/1/activo", ADMIN, "{\"activo\":false}").andExpect(status().isOk());
        enviar("PATCH", "/api/admin/empleados/1/rol", ADMIN, "{\"rol\":\"ADMINISTRADOR\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
        enviar("GET", "/api/auth/me", VENDEDOR, null).andExpect(status().isUnauthorized());
        enviar("PATCH", "/api/admin/empleados/1/activo", ADMIN, "{\"activo\":true}").andExpect(status().isOk());

        CountDownLatch inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> primero = executor.submit(() -> {
                inicio.await();
                return enviar("PATCH", "/api/admin/empleados/2/rol", ADMIN, "{\"rol\":\"VENDEDOR\"}")
                        .andReturn().getResponse().getStatus();
            });
            Future<Integer> segundo = executor.submit(() -> {
                inicio.await();
                return enviar("PATCH", "/api/admin/empleados/1/rol", ADMIN, "{\"rol\":\"VENDEDOR\"}")
                        .andReturn().getResponse().getStatus();
            });
            inicio.countDown();
            List<Integer> resultados = List.of(primero.get(20, TimeUnit.SECONDS), segundo.get(20, TimeUnit.SECONDS));
            assertThat(resultados).contains(200);
            assertThat(resultados).allMatch(estado -> estado == 200 || estado == 409 || estado == 403);
        }
        assertThat(jdbc.queryForObject("select count(*) from usuarios where rol='ADMINISTRADOR' and activo=1", Long.class))
                .isEqualTo(1);
        String sobreviviente = jdbc.queryForObject("select nombre_usuario from usuarios where rol='ADMINISTRADOR' and activo=1", String.class);
        String autenticacion = "personal".equals(sobreviviente) ? ADMIN : VENDEDOR;
        enviar("PATCH", "/api/admin/empleados/2/rol", autenticacion, "{\"rol\":\"ADMINISTRADOR\"}")
                .andExpect(status().isOk());
        enviar("PATCH", "/api/admin/empleados/1/rol", ADMIN, "{\"rol\":\"ADMINISTRADOR\"}")
                .andExpect(status().isOk());
    }

    @Test
    @Order(2)
    void perfilYRolPermanecenDespuesDeReiniciar() throws Exception {
        enviar("GET", "/api/admin/empleados/1", ADMIN, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("ana.nueva@example.com"))
                .andExpect(jsonPath("$.telefono").value("999777666"))
                .andExpect(jsonPath("$.dni").value("12345678"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
        enviar("GET", "/api/admin/usuarios", VENDEDOR, null).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from usuarios", Long.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from empleados", Long.class)).isEqualTo(2);
    }

    private ResultActions enviar(String metodo, String ruta, String auth, String cuerpo) throws Exception {
        var solicitud = switch (metodo) {
            case "POST" -> post(ruta);
            case "PUT" -> put(ruta);
            case "PATCH" -> patch(ruta);
            default -> get(ruta);
        };
        if (auth != null) solicitud.header(HttpHeaders.AUTHORIZATION, auth);
        if (cuerpo != null) solicitud.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        return mvc.perform(solicitud);
    }

    private static String basic(String credenciales) {
        return "Basic " + Base64.getEncoder().encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));
    }
}


