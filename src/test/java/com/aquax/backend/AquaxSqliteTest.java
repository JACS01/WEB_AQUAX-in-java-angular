package com.aquax.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
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
class AquaxSqliteTest {

    private static final Path DB = Path.of("target", "sqlite-" + UUID.randomUUID() + ".db").toAbsolutePath();
    private static final String ADMIN = basic("personal:1234");
    private static final String ADMIN_NUEVO = basic("personal:SQLiteNueva2026!");
    private static final String VENDEDOR = basic("vendedor1:ClaveVendedor2026!");
    private static final String PEDIDO = """
            {"nombreCliente":"Cliente SQLite","telefonoCliente":"999888777","direccionEntrega":"Direccion SQLite",
            "detalles":[{"productoId":1,"cantidad":2},{"productoId":2,"cantidad":1}]}
            """;
    private static final String PREFERENCIAS = """
            {"idioma":"en","nivelZoom":120,"filtroColor":"grayscale","filtroColorActivo":true}
            """;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @DynamicPropertySource
    static void configuracion(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DB);
    }

    @Test
    @Order(1)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void datosSeGuardanEnArchivoYLasOperacionesSonAtomicas() throws Exception {
        assertThat(jdbc.queryForObject("PRAGMA foreign_keys", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForList("PRAGMA foreign_key_list(notificaciones)")).hasSize(2);
        assertThat(jdbc.queryForList("PRAGMA foreign_key_list(detalles_pedido)")).hasSize(2);
        assertThat(jdbc.queryForList("PRAGMA foreign_key_list(historial_modificaciones)")).hasSize(2);
        assertThat(jdbc.queryForList("PRAGMA index_list(notificaciones)")).isNotEmpty();
        assertThat(jdbc.queryForList("PRAGMA foreign_key_check")).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from productos", Long.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("select count(*) from ajustes_empresariales", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from usuarios", Long.class)).isEqualTo(1);
        assertThatThrownBy(() -> jdbc.update("insert into empleados(dni,fecha_ingreso,usuario_id) values('87654321','2026-11-11',99999)"))
                .isInstanceOf(org.springframework.dao.DataAccessException.class)
                .hasMessageContaining("FOREIGN KEY");

        enviar("POST", "/api/admin/usuarios", ADMIN, """
                {"nombreUsuario":"vendedor1","nombre":"Ana","apellido":"Perez","correo":"ana@example.com",
                "contrasena":"ClaveVendedor2026!","rol":"VENDEDOR"}
                """).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(2));
        enviar("POST", "/api/admin/empleados", ADMIN, """
                {"usuarioId":2,"dni":"12345678","fechaIngreso":"2026-11-01"}
                """).andExpect(status().isCreated());
        enviar("PUT", "/api/personal/perfil", ADMIN, """
                {"nombre":"Perfil Persistente","apellido":"AquaX","correo":"persistente@example.com","telefono":"999888777"}
                """).andExpect(status().isOk());
        enviar("PUT", "/api/personal/preferencias", ADMIN, PREFERENCIAS).andExpect(status().isOk());
        enviar("POST", "/api/pedidos", null, PEDIDO).andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroPedido").value("AQX-0001"))
                .andExpect(jsonPath("$.total").value(32));
        enviar("POST", "/api/personal/pedidos", VENDEDOR, PEDIDO).andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroPedido").value("AQX-0002"));
        enviar("PATCH", "/api/personal/pedidos/2/entrega", ADMIN, "{\"estadoEntrega\":\"EN_CAMINO\"}")
                .andExpect(status().isOk());
        enviar("PATCH", "/api/personal/pedidos/2/entrega", VENDEDOR, "{\"estadoEntrega\":\"ENTREGADO\"}")
                .andExpect(status().isOk());
        enviar("PATCH", "/api/personal/pedidos/2/pago", ADMIN, "{\"estadoPago\":\"PAGADO\"}")
                .andExpect(status().isOk());
        enviar("GET", "/api/personal/pedidos/2/historial", ADMIN, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].usuario").value("vendedor1"));
        enviar("PATCH", "/api/personal/notificaciones/leidas", ADMIN, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.actualizadas").value(1));

        assertThatThrownBy(() -> jdbc.update(
                "insert into notificaciones(pedido_id,usuario_id,titulo,mensaje,tipo,leida,fecha_creacion) "
                + "select pedido_id,usuario_id,titulo,mensaje,tipo,leida,fecha_creacion from notificaciones limit 1"))
                .isInstanceOf(org.springframework.dao.DataAccessException.class)
                .hasMessageContaining("UNIQUE");
        long pedidosAntes = contar("pedidos");
        long detallesAntes = contar("detalles_pedido");
        long avisosAntes = contar("notificaciones");
        jdbc.execute("CREATE TRIGGER forzar_fallo_notificacion BEFORE INSERT ON notificaciones BEGIN SELECT RAISE(ABORT,'fallo controlado'); END");
        try {
            enviar("POST", "/api/pedidos", null, PEDIDO)
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.title").value("Error interno del servidor"))
                    .andExpect(jsonPath("$.detail").value("Ocurrió un error interno. Inténtalo nuevamente más tarde."))
                    .andExpect(jsonPath("$.instance").value("/api/pedidos"));
        } finally {
            jdbc.execute("DROP TRIGGER forzar_fallo_notificacion");
        }
        assertThat(contar("pedidos")).isEqualTo(pedidosAntes);
        assertThat(contar("detalles_pedido")).isEqualTo(detallesAntes);
        assertThat(contar("notificaciones")).isEqualTo(avisosAntes);

        enviar("PUT", "/api/admin/empresa", ADMIN, """
                {"razonSocial":"AQUAX Persistente","ruc":"10715736689","direccion":"Direccion permanente",
                "telefono1":"999888777","telefono2":null,"correo":"empresa@example.com",
                "registroSanitario":"Registro de prueba","slogan":"Guardado en SQLite"}
                """).andExpect(status().isOk());
        jdbc.update("update productos set precio = ? where id = 1", "99999999.99");
        var grande = enviar("POST", "/api/pedidos", null, """
                {"nombreCliente":"Monto exacto","telefonoCliente":"999888777","direccionEntrega":"Direccion SQLite",
                 "detalles":[{"productoId":1,"cantidad":30}]}
                """).andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroPedido").value("AQX-0003"))
                .andReturn().getResponse().getContentAsString();
        assertThat(grande).contains("\"total\":2999999999.70");
        assertThat(jdbc.queryForObject("select subtotal from detalles_pedido where pedido_id=3", String.class))
                .isEqualTo("2999999999.70");
        // Simula un pedido histórico para verificar su recuperación después de reiniciar.
        jdbc.update("update detalles_pedido set cantidad = ?, subtotal = ? where pedido_id = 3",
                Integer.MAX_VALUE, "214748364678525163.53");
        jdbc.update("update productos set precio = ? where id = 1", "13.27");

        enviar("POST", "/api/admin/usuarios", ADMIN, """
                {"nombreUsuario":"concurrente","nombre":"Cuenta","apellido":"Concurrente","correo":"concurrente@example.com",
                "contrasena":"ClaveVendedor2026!","rol":"VENDEDOR"}
                """).andExpect(status().isCreated());
        String concurrente = basic("concurrente:ClaveVendedor2026!");
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            CountDownLatch inicio = new CountDownLatch(1);
            var uno = ejecutor.submit(() -> { inicio.await(); return enviar("PUT", "/api/personal/preferencias", concurrente, PREFERENCIAS)
                    .andReturn().getResponse().getStatus(); });
            var dos = ejecutor.submit(() -> { inicio.await(); return enviar("PUT", "/api/personal/preferencias", concurrente, PREFERENCIAS)
                    .andReturn().getResponse().getStatus(); });
            inicio.countDown();
            assertThat(uno.get(40, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(dos.get(40, TimeUnit.SECONDS)).isEqualTo(200);
        }
        assertThat(jdbc.queryForObject("select count(*) from configuraciones_usuario where usuario_id = 3", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForList("PRAGMA foreign_key_check")).isEmpty();
        enviar("PATCH", "/api/personal/perfil/contrasena", ADMIN, """
                {"contrasenaActual":"1234","nuevaContrasena":"SQLiteNueva2026!","confirmarContrasena":"SQLiteNueva2026!"}
                """).andExpect(status().isOk());
        enviar("GET", "/api/auth/me", ADMIN, null).andExpect(status().isUnauthorized());
        enviar("GET", "/api/auth/me", ADMIN_NUEVO, null).andExpect(status().isOk());
    }

    @Test
    @Order(2)
    void reiniciarConservaDatosYNoReiniciaLasCredencialesNiLasSemillas() throws Exception {
        assertThat(DB).exists();
        enviar("GET", "/api/auth/me", ADMIN, null).andExpect(status().isUnauthorized());
        enviar("GET", "/api/personal/perfil", ADMIN_NUEVO, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Perfil Persistente"))
                .andExpect(jsonPath("$.correo").value("persistente@example.com"));
        enviar("GET", "/api/personal/preferencias", ADMIN_NUEVO, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.idioma").value("en"))
                .andExpect(jsonPath("$.nivelZoom").value(120));
        enviar("GET", "/api/admin/empresa", ADMIN_NUEVO, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.razonSocial").value("AQUAX Persistente"))
                .andExpect(jsonPath("$.slogan").value("Guardado en SQLite"));
        enviar("GET", "/api/admin/empleados", ADMIN_NUEVO, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].dni").value("12345678"));
        enviar("GET", "/api/personal/pedidos?completados=true", VENDEDOR, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].numeroPedido").value("AQX-0002"))
                .andExpect(jsonPath("$[0].total").value(32));
        enviar("GET", "/api/personal/pedidos/2/historial", VENDEDOR, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
        enviar("GET", "/api/personal/notificaciones/no-leidas", ADMIN_NUEVO, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(1));
        enviar("GET", "/api/personal/notificaciones/no-leidas", VENDEDOR, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(2));
        assertThat(contar("productos")).isEqualTo(3);
        assertThat(contar("ajustes_empresariales")).isEqualTo(1);
        assertThat(contar("usuarios")).isEqualTo(3);
        assertThat(contar("pedidos")).isEqualTo(3);
        assertThat(contar("detalles_pedido")).isEqualTo(5);
        assertThat(contar("notificaciones")).isEqualTo(4);
        assertThat(contar("historial_modificaciones")).isEqualTo(4);
        assertThat(jdbc.queryForObject("select precio from productos where id=1", java.math.BigDecimal.class))
                .isEqualByComparingTo("13.27");
        assertThat(jdbc.queryForObject("select subtotal from detalles_pedido where pedido_id=3", String.class))
                .isEqualTo("214748364678525163.53");
        assertThat(jdbc.queryForObject("select typeof(subtotal) from detalles_pedido where pedido_id=3", String.class))
                .isEqualTo("text");
        enviar("GET", "/api/personal/pedidos", ADMIN_NUEVO, null).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("214748364678525163.53")));
        assertThat(jdbc.queryForList("PRAGMA foreign_key_check")).isEmpty();
        enviar("POST", "/api/pedidos", null, PEDIDO).andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroPedido").value("AQX-0004"))
                .andExpect(jsonPath("$.total").value(38.54));
    }

    private long contar(String tabla) {
        return jdbc.queryForObject("select count(*) from " + tabla, Long.class);
    }

    private ResultActions enviar(String metodo, String ruta, String autorizacion, String cuerpo) throws Exception {
        var solicitud = request(org.springframework.http.HttpMethod.valueOf(metodo), ruta);
        if (autorizacion != null) solicitud.header(HttpHeaders.AUTHORIZATION, autorizacion);
        if (cuerpo != null) solicitud.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        return mvc.perform(solicitud);
    }

    private static String basic(String credenciales) {
        return "Basic " + Base64.getEncoder().encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));
    }
}


