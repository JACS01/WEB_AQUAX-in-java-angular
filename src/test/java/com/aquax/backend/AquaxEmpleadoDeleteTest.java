package com.aquax.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.ServletException;

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
class AquaxEmpleadoDeleteTest {

    private static final Path DB = Path.of("target", "empleado-delete-" + UUID.randomUUID() + ".db").toAbsolutePath();
    private static final String ADMIN = basic("personal:1234");
    private static final String SELLER = basic("empleado_baja:ClaveVendedor2026!");
    private static long usuarioId;
    private static long empleadoId;
    private static String hashOriginal;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DB);
    }

    @Test
    @Order(1)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void eliminaSoloFichaLaboralYConservaAccesoHistorialYRelaciones() throws Exception {
        solicitud("POST", "/api/admin/empleados/con-usuario", ADMIN, """
                {
                  "usuario":{"nombreUsuario":"empleado_baja","nombre":"Ana","apellido":"Perez",
                    "correo":"empleado.baja@example.com","contrasena":"ClaveVendedor2026!","rol":"VENDEDOR"},
                  "telefono":"999888777","dni":"12345678"
                }
                """).andExpect(status().isCreated());
        usuarioId = jdbc.queryForObject("select id from usuarios where nombre_usuario = 'empleado_baja'", Long.class);
        empleadoId = jdbc.queryForObject("select id from empleados where usuario_id = ?", Long.class, usuarioId);
        hashOriginal = jdbc.queryForObject("select contrasena from usuarios where id = ?", String.class, usuarioId);

        solicitud("PUT", "/api/personal/preferencias", SELLER,
                "{\"idioma\":\"en\",\"nivelZoom\":120,\"filtroColor\":\"grayscale\",\"filtroColorActivo\":true}")
                .andExpect(status().isOk());
        String pedido = """
                {"nombreCliente":"Cliente prueba baja","telefonoCliente":"999888777",
                 "direccionEntrega":"Direccion prueba","detalles":[{"productoId":1,"cantidad":2}]}
                """;
        solicitud("POST", "/api/personal/pedidos", SELLER, pedido).andExpect(status().isCreated());
        solicitud("POST", "/api/pedidos", null, pedido).andExpect(status().isCreated());
        assertThat(contar("pedidos")).isEqualTo(2);
        assertThat(contar("historial_modificaciones")).isEqualTo(1);
        assertThat(contar("notificaciones")).isEqualTo(2);

        String ruta = "/api/admin/empleados/" + empleadoId;
        solicitud("DELETE", ruta, null, null).andExpect(status().isUnauthorized());
        solicitud("DELETE", ruta, SELLER, null).andExpect(status().isForbidden());
        solicitud("DELETE", "/api/admin/empleados/99999", ADMIN, null).andExpect(status().isNotFound());
        assertThat(contar("empleados")).isEqualTo(1);

        jdbc.execute("""
                create trigger bloquear_baja before delete on empleados
                begin select raise(abort, 'fallo de borrado de prueba'); end
                """);
        try {
            int estado = solicitud("DELETE", ruta, ADMIN, null).andReturn().getResponse().getStatus();
            assertThat(estado).isGreaterThanOrEqualTo(400);
        } catch (ServletException esperado) {
            assertThat(esperado.getCause()).isNotNull();
        } finally {
            jdbc.execute("drop trigger bloquear_baja");
        }
        assertThat(contar("empleados")).isEqualTo(1);
        assertThat(contar("usuarios")).isEqualTo(2);

        solicitud("DELETE", ruta, ADMIN, null)
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        solicitud("DELETE", ruta, ADMIN, null).andExpect(status().isNotFound());
        solicitud("GET", ruta, ADMIN, null).andExpect(status().isNotFound());
        solicitud("GET", "/api/admin/empleados", ADMIN, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        assertThat(contar("usuarios")).isEqualTo(2);
        assertThat(contar("configuraciones_usuario")).isEqualTo(1);
        assertThat(contar("pedidos")).isEqualTo(2);
        assertThat(contar("detalles_pedido")).isEqualTo(2);
        assertThat(contar("historial_modificaciones")).isEqualTo(1);
        assertThat(contar("notificaciones")).isEqualTo(2);
        assertThat(jdbc.queryForObject("select contrasena from usuarios where id = ?", String.class, usuarioId))
                .isEqualTo(hashOriginal);
        assertThat(jdbc.queryForObject("select rol from usuarios where id = ?", String.class, usuarioId))
                .isEqualTo("VENDEDOR");
        assertThat(jdbc.queryForObject("select activo from usuarios where id = ?", Boolean.class, usuarioId))
                .isTrue();
        solicitud("GET", "/api/personal/perfil", SELLER, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.telefono").value("999888777"));
        solicitud("GET", "/api/personal/preferencias", SELLER, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.idioma").value("en"));
        solicitud("GET", "/api/personal/pedidos/1/historial", SELLER, null).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").value(usuarioId));
        solicitud("GET", "/api/personal/notificaciones", SELLER, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        assertThat(jdbc.queryForList("pragma foreign_key_check")).isEmpty();

        solicitud("POST", "/api/admin/empleados", ADMIN,
                "{\"usuarioId\":" + usuarioId + ",\"dni\":\"12345678\"}")
                .andExpect(status().isCreated());
        long nuevoId = jdbc.queryForObject("select id from empleados where usuario_id = ?", Long.class, usuarioId);
        assertThat(nuevoId).isGreaterThan(empleadoId);
        var inicio = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var uno = pool.submit(() -> { inicio.await(); return eliminar(nuevoId); });
            var dos = pool.submit(() -> { inicio.await(); return eliminar(nuevoId); });
            inicio.countDown();
            assertThat(new int[] {uno.get(20, TimeUnit.SECONDS), dos.get(20, TimeUnit.SECONDS)})
                    .containsExactlyInAnyOrder(204, 404);
        }
        assertThat(contar("empleados")).isZero();

        solicitud("POST", "/api/admin/empleados", ADMIN, "{\"usuarioId\":1,\"dni\":\"87654321\"}")
                .andExpect(status().isCreated());
        long adminEmpleado = jdbc.queryForObject("select id from empleados where usuario_id = 1", Long.class);
        assertThat(eliminar(adminEmpleado)).isEqualTo(204);
        solicitud("GET", "/api/auth/me", ADMIN, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.activo").value(true));
        assertThat(jdbc.queryForObject(
                "select count(*) from usuarios where rol = 'ADMINISTRADOR' and activo = 1", Long.class)).isEqualTo(1);
    }

    @Test
    @Order(2)
    void reinicioMantieneBajaYCuentasRelacionadas() throws Exception {
        assertThat(contar("empleados")).isZero();
        assertThat(contar("usuarios")).isEqualTo(2);
        assertThat(contar("pedidos")).isEqualTo(2);
        assertThat(contar("notificaciones")).isEqualTo(2);
        solicitud("GET", "/api/admin/empleados/" + empleadoId, ADMIN, null).andExpect(status().isNotFound());
        solicitud("GET", "/api/personal/perfil", SELLER, null).andExpect(status().isOk());
        solicitud("GET", "/api/personal/preferencias", SELLER, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.idioma").value("en"));
        assertThat(jdbc.queryForObject("select contrasena from usuarios where id = ?", String.class, usuarioId))
                .isEqualTo(hashOriginal);
        assertThat(jdbc.queryForList("pragma foreign_key_check")).isEmpty();
    }

    private long contar(String tabla) {
        return jdbc.queryForObject("select count(*) from " + tabla, Long.class);
    }

    private int eliminar(long id) throws Exception {
        return solicitud("DELETE", "/api/admin/empleados/" + id, ADMIN, null).andReturn().getResponse().getStatus();
    }

    private ResultActions solicitud(String metodo, String ruta, String auth, String cuerpo) throws Exception {
        var request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .request(org.springframework.http.HttpMethod.valueOf(metodo), ruta);
        if (auth != null) request.header(HttpHeaders.AUTHORIZATION, auth);
        if (cuerpo != null) request.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        return mvc.perform(request);
    }

    private static String basic(String credenciales) {
        return "Basic " + Base64.getEncoder().encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));
    }
}


