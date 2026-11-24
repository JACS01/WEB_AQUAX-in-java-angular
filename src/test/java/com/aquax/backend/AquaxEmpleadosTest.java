package com.aquax.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = {"aquax.personal.usuario=personal", "aquax.personal.contrasena=1234", "aquax.jwt.secret-base64=", "aquax.jwt.expiration-seconds=3600", "aquax.cors.allowed-origins=http://localhost:4200"})
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("sqlite")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class AquaxEmpleadosTest {
    private static final java.nio.file.Path AUDITORIA_DB = java.nio.file.Path.of(
            "target", "auditoria-" + java.util.UUID.randomUUID() + ".db").toAbsolutePath();

    @org.springframework.test.context.DynamicPropertySource
    static void configurarAuditoria(org.springframework.test.context.DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> "jdbc:sqlite:" + AUDITORIA_DB);
    }

    private static final String RUTA = "/api/admin/empleados";
    private static final String ADMIN = basic("personal:1234");
    private static final String VENDEDOR = basic("vendedor1:clave-vendedor");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;

    @Test
    void administracionVinculaEmpleadosSinDuplicarPersonasYConservaSusCuentas() throws Exception {
        crearUsuario("vendedor1", "ana@example.com", "VENDEDOR");
        long vendedorId = usuarioId("vendedor1");

        mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                .content(vinculo(vendedorId, "12345678"))).andExpect(status().isUnauthorized());
        enviar(get(RUTA), VENDEDOR).andExpect(status().isForbidden());
        enviar(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(vinculo(vendedorId, "12345678")),
                VENDEDOR).andExpect(status().isForbidden());

        long usuariosAntes = cantidad("usuarios");
        enviar(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(vinculo(vendedorId, "12345678")), ADMIN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(vendedorId))
                .andExpect(jsonPath("$.nombreUsuario").value("vendedor1"))
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.correo").value("ana@example.com"))
                .andExpect(jsonPath("$.telefono").doesNotExist())
                .andExpect(jsonPath("$.rol").value("VENDEDOR"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.fechaIngreso").value(LocalDate.now(ZoneId.of("America/Lima")).toString()))
                .andExpect(jsonPath("$.contrasena").doesNotExist());
        assertThat(cantidad("usuarios")).isEqualTo(usuariosAntes);
        long empleadoId = empleadoId(vendedorId);
        enviar(get(RUTA), ADMIN).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        enviar(get(RUTA + "/" + empleadoId), ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.dni").value("12345678"));
        enviar(get(RUTA + "/99999"), ADMIN).andExpect(status().isNotFound());
        enviar(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(vinculo(99999, "22345678")), ADMIN)
                .andExpect(status().isNotFound());
        enviar(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(vinculo(vendedorId, "22345678")), ADMIN)
                .andExpect(status().isConflict());
        enviar(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(vinculo(1, "12345678")), ADMIN)
                .andExpect(status().isConflict());
        enviar(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(vinculo(1, "12")), ADMIN)
                .andExpect(status().isBadRequest());
        assertThat(cantidad("empleados")).isEqualTo(1);

        enviar(put(RUTA + "/" + empleadoId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"dni\":\"87654321\",\"fechaIngreso\":\"2025-02-01\"}"), ADMIN)
                .andExpect(status().isOk()).andExpect(jsonPath("$.dni").value("87654321"))
                .andExpect(jsonPath("$.fechaIngreso").value("2025-02-01"));
        enviar(put(RUTA + "/" + empleadoId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"dni\":\"77777777\"}"), ADMIN).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select dni from empleados where id = ?", String.class, empleadoId))
                .isEqualTo("87654321");

        enviar(put("/api/personal/perfil").contentType(MediaType.APPLICATION_JSON).content("""
                {"nombre":"Ana actualizada","apellido":"Pérez","correo":"nueva@example.com","telefono":"999888777"}
                """), VENDEDOR).andExpect(status().isOk());
        enviar(get(RUTA + "/" + empleadoId), ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana actualizada"))
                .andExpect(jsonPath("$.correo").value("nueva@example.com"))
                .andExpect(jsonPath("$.telefono").value("999888777"));

        enviar(patch(RUTA + "/" + empleadoId + "/activo").contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":false}"), ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        enviar(get("/api/personal/pedidos"), VENDEDOR).andExpect(status().isUnauthorized());
        enviar(patch("/api/admin/usuarios/" + vendedorId + "/activo").contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":true}"), ADMIN).andExpect(status().isOk());
        enviar(get(RUTA + "/" + empleadoId), ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
        enviar(get("/api/personal/pedidos"), VENDEDOR).andExpect(status().isOk());
        assertThat(encoder.matches("clave-vendedor", jdbc.queryForObject(
                "select contrasena from usuarios where id=?", String.class, vendedorId))).isTrue();

        enviar(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(vinculo(1, "55555555")), ADMIN)
                .andExpect(status().isCreated());
        long adminEmpleadoId = empleadoId(1);
        enviar(patch(RUTA + "/" + adminEmpleadoId + "/activo").contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":false}"), ADMIN).andExpect(status().isConflict());
        enviar(get("/api/auth/me"), ADMIN).andExpect(status().isOk());

        enviar(post(RUTA + "/con-usuario").contentType(MediaType.APPLICATION_JSON)
                .content(empleadoNuevo("vendedor2", "nuevo@example.com", "22222222")), ADMIN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreUsuario").value("vendedor2"))
                .andExpect(jsonPath("$.telefono").value("999111222"))
                .andExpect(jsonPath("$.fechaIngreso").value("2024-03-10"))
                .andExpect(jsonPath("$.contrasena").doesNotExist());
        long nuevoUsuarioId = usuarioId("vendedor2");
        assertThat(encoder.matches("clave-vendedor", jdbc.queryForObject(
                "select contrasena from usuarios where id=?", String.class, nuevoUsuarioId))).isTrue();
        enviar(get("/api/personal/pedidos"), basic("vendedor2:clave-vendedor")).andExpect(status().isOk());
        long empleadosAntes = cantidad("empleados");
        usuariosAntes = cantidad("usuarios");
        enviar(post(RUTA + "/con-usuario").contentType(MediaType.APPLICATION_JSON)
                .content(empleadoNuevo("duplicado", "duplicado@example.com", "22222222")), ADMIN)
                .andExpect(status().isConflict());
        assertThat(cantidad("empleados")).isEqualTo(empleadosAntes);
        assertThat(cantidad("usuarios")).isEqualTo(usuariosAntes);

        jdbc.execute("CREATE TRIGGER prueba_empleado_falla BEFORE INSERT ON empleados WHEN NEW.dni = '66666666' BEGIN SELECT RAISE(ABORT, 'Fallo controlado'); END");
        try {
            enviar(post(RUTA + "/con-usuario").contentType(MediaType.APPLICATION_JSON)
                    .content(empleadoNuevo("rollback", "rollback@example.com", "66666666")), ADMIN)
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.title").value("Error interno del servidor"))
                    .andExpect(jsonPath("$.detail").value("Ocurrió un error interno. Inténtalo nuevamente más tarde."))
                    .andExpect(jsonPath("$.instance").value(RUTA + "/con-usuario"));
        } finally {
            jdbc.execute("DROP TRIGGER prueba_empleado_falla");
        }
        assertThat(cantidad("empleados")).isEqualTo(empleadosAntes);
        assertThat(cantidad("usuarios")).isEqualTo(usuariosAntes);
        assertThat(jdbc.queryForObject("select count(*) from usuarios where nombre_usuario='rollback'", Long.class))
                .isZero();

        crearUsuario("concurrente", "concurrente@example.com", "VENDEDOR");
        long concurrente = usuarioId("concurrente");
        CountDownLatch iniciar = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Future<Integer> primera = pool.submit(() -> {
                iniciar.await();
                return enviar(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content(vinculo(concurrente, "33333333")), ADMIN).andReturn().getResponse().getStatus();
            });
            Future<Integer> segunda = pool.submit(() -> {
                iniciar.await();
                return enviar(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content(vinculo(concurrente, "44444444")), ADMIN).andReturn().getResponse().getStatus();
            });
            iniciar.countDown();
            assertThat(List.of(primera.get(20, TimeUnit.SECONDS), segunda.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(jdbc.queryForObject(
                "select count(*) from empleados where usuario_id=?", Long.class, concurrente)).isEqualTo(1);
    }

    private ResultActions enviar(MockHttpServletRequestBuilder solicitud, String auth) throws Exception {
        return mvc.perform(solicitud.header(HttpHeaders.AUTHORIZATION, auth));
    }

    private void crearUsuario(String nombreUsuario, String correo, String rol) throws Exception {
        enviar(post("/api/admin/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
                {"nombreUsuario":"%s","nombre":"Ana","apellido":"Pérez","correo":"%s",
                 "contrasena":"clave-vendedor","rol":"%s"}
                """.formatted(nombreUsuario, correo, rol)), ADMIN).andExpect(status().isCreated());
    }

    private static String vinculo(long usuarioId, String dni) {
        return "{\"usuarioId\":%d,\"dni\":\"%s\"}".formatted(usuarioId, dni);
    }

    private static String empleadoNuevo(String nombreUsuario, String correo, String dni) {
        return """
                {"usuario":{"nombreUsuario":"%s","nombre":"Nuevo","apellido":"Pérez","correo":"%s",
                 "contrasena":"clave-vendedor","rol":"VENDEDOR"},
                 "telefono":"999111222","dni":"%s","fechaIngreso":"2024-03-10"}
                """.formatted(nombreUsuario, correo, dni);
    }

    private long usuarioId(String nombreUsuario) {
        return jdbc.queryForObject("select id from usuarios where nombre_usuario=?", Long.class, nombreUsuario);
    }

    private long empleadoId(long usuarioId) {
        return jdbc.queryForObject("select id from empleados where usuario_id=?", Long.class, usuarioId);
    }

    private long cantidad(String tabla) {
        return jdbc.queryForObject("select count(*) from " + tabla, Long.class);
    }

    private static String basic(String credenciales) {
        return "Basic " + Base64.getEncoder().encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));
    }
}


