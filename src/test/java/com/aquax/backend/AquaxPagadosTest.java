package com.aquax.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

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
class AquaxPagadosTest {

    private static final Path DB = Path.of("target", "pagados-" + UUID.randomUUID() + ".db").toAbsolutePath();
    private static final String RUTA = "/api/personal/pedidos/pagados";
    private static final String ADMIN = basic("personal:1234");
    private static final String VENDEDOR = basic("vendedor1:ClaveVendedor2026!");
    private static final String PEDIDO = """
            {"nombreCliente":"Cliente pagados","telefonoCliente":"999888777","direccionEntrega":"Direccion pagados",
            "detalles":[{"productoId":1,"cantidad":2},{"productoId":2,"cantidad":1}]}
            """;
    private static final BigDecimal MONTO_FINAL = new BigDecimal("214748364678525394.07");
    private static long primeroPorFecha;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> "jdbc:sqlite:" + DB);
    }

    @Test
    @Order(1)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void historialIncluyeTodosLosPagadosYUsaPreciosHistoricosExactos() throws Exception {
        solicitud("GET", RUTA, null, null).andExpect(status().isUnauthorized());
        solicitud("GET", RUTA, basic("personal:incorrecta"), null).andExpect(status().isUnauthorized());
        historial(ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPagados").value(0))
                .andExpect(jsonPath("$.montoTotal").value(0))
                .andExpect(jsonPath("$.pedidos.length()").value(0));
        solicitud("POST", "/api/admin/usuarios", ADMIN, """
                {"nombreUsuario":"vendedor1","nombre":"Ana","apellido":"Perez","correo":"ana@example.com",
                "contrasena":"ClaveVendedor2026!","rol":"VENDEDOR"}
                """).andExpect(status().isCreated());
        historial(VENDEDOR).andExpect(status().isOk());

        List<Long> pagados = new ArrayList<>();
        List<Long> pendientes = new ArrayList<>();
        for (String origen : new String[] {"CLIENTE", "TRABAJADOR"}) {
            for (String entrega : new String[] {"NUEVO", "EN_CAMINO", "ENTREGADO"}) {
                for (boolean pagado : new boolean[] {true, false}) {
                    long id = crear(origen, PEDIDO);
                    if (!entrega.equals("NUEVO")) {
                        entrega(id, entrega);
                    }
                    if (pagado) {
                        pago(id, "PAGADO");
                        pagados.add(id);
                    } else {
                        pendientes.add(id);
                    }
                }
            }
        }
        assertThat(jdbc.queryForObject("select count(*) from detalles_pedido", Long.class)).isEqualTo(24L);
        historial(ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPagados").value(6))
                .andExpect(jsonPath("$.montoTotal").value(192))
                .andExpect(jsonPath("$.pedidos.length()").value(6))
                .andExpect(jsonPath("$.pedidos[0].id").value(pagados.getLast()))
                .andExpect(jsonPath("$.pedidos[0].origen").value("TRABAJADOR"))
                .andExpect(jsonPath("$.pedidos[0].usuarioRegistro").value("personal"))
                .andExpect(jsonPath("$.pedidos[0].detalles.length()").value(2))
                .andExpect(jsonPath("$.pedidos[0].contrasena").doesNotExist());
        historial(VENDEDOR).andExpect(status().isOk()).andExpect(jsonPath("$.montoTotal").value(192));
        solicitud("GET", "/api/personal/pedidos?completados=true", ADMIN, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        solicitud("GET", "/api/personal/dashboard/resumen", ADMIN, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(10))
                .andExpect(jsonPath("$.pendientes").value(6))
                .andExpect(jsonPath("$.pagados").value(4))
                .andExpect(jsonPath("$.deClientes").value(5));

        pago(pagados.getFirst(), "PENDIENTE");
        historial(ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPagados").value(5))
                .andExpect(jsonPath("$.montoTotal").value(160));
        pago(pendientes.getFirst(), "PAGADO");
        historial(ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPagados").value(6))
                .andExpect(jsonPath("$.montoTotal").value(192));
        primeroPorFecha = pagados.get(1);
        entrega(primeroPorFecha, "ENTREGADO");
        historial(ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPagados").value(6))
                .andExpect(jsonPath("$.montoTotal").value(192));

        jdbc.update("update productos set precio = ? where id = 1", new BigDecimal("13.27"));
        historial(ADMIN).andExpect(status().isOk()).andExpect(jsonPath("$.montoTotal").value(192));
        long fraccionario = crear("CLIENTE", PEDIDO);
        pago(fraccionario, "PAGADO");
        historial(ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPagados").value(7))
                .andExpect(jsonPath("$.montoTotal").value(230.54));

        jdbc.update("update productos set precio = ? where id = 1", new BigDecimal("99999999.99"));
        long extremo = crear("CLIENTE", """
                {"nombreCliente":"Cliente extremo","telefonoCliente":"999888777","direccionEntrega":"Direccion",
                "detalles":[{"productoId":1,"cantidad":2147483647}]}
                """);
        pago(extremo, "PAGADO");
        jdbc.update("update pedidos set fecha_registro = ? where id = ?",
                java.sql.Date.valueOf("2030-01-01"), primeroPorFecha);
        verificarMontoFinal();
        historial(ADMIN).andExpect(jsonPath("$.pedidos[0].id").value(primeroPorFecha));
        assertThat(new BigDecimal(jdbc.queryForObject(
                "select subtotal from detalles_pedido where pedido_id = ?", String.class, extremo)))
                .isEqualByComparingTo("214748364678525163.53");
        solicitud("PATCH", "/api/admin/usuarios/2/activo", ADMIN, "{\"activo\":false}")
                .andExpect(status().isOk());
        historial(VENDEDOR).andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    void historialPagadoPersisteDespuesDeReiniciar() throws Exception {
        verificarMontoFinal();
        historial(ADMIN).andExpect(jsonPath("$.pedidos[0].id").value(primeroPorFecha));
        assertThat(jdbc.queryForObject("select count(*) from productos", Long.class)).isEqualTo(3L);
        assertThat(jdbc.queryForObject("select count(*) from pedidos", Long.class)).isEqualTo(14L);
    }

    private void verificarMontoFinal() throws Exception {
        String body = historial(ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPagados").value(8))
                .andExpect(jsonPath("$.pedidos.length()").value(8))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("\"montoTotal\":" + MONTO_FINAL.toPlainString());
    }

    private long crear(String origen, String body) throws Exception {
        solicitud("POST", origen.equals("CLIENTE") ? "/api/pedidos" : "/api/personal/pedidos",
                origen.equals("CLIENTE") ? null : ADMIN, body).andExpect(status().isCreated());
        return jdbc.queryForObject("select max(id) from pedidos", Long.class);
    }

    private void pago(long id, String estado) throws Exception {
        solicitud("PATCH", "/api/personal/pedidos/" + id + "/pago", ADMIN,
                "{\"estadoPago\":\"" + estado + "\"}").andExpect(status().isOk());
    }

    private void entrega(long id, String estado) throws Exception {
        solicitud("PATCH", "/api/personal/pedidos/" + id + "/entrega", ADMIN,
                "{\"estadoEntrega\":\"" + estado + "\"}").andExpect(status().isOk());
    }

    private ResultActions historial(String auth) throws Exception {
        return solicitud("GET", RUTA, auth, null);
    }

    private ResultActions solicitud(String metodo, String ruta, String auth, String body) throws Exception {
        var request = switch (metodo) {
            case "GET" -> get(ruta);
            case "POST" -> post(ruta);
            case "PATCH" -> patch(ruta);
            default -> throw new IllegalArgumentException(metodo);
        };
        if (auth != null) {
            request.header(HttpHeaders.AUTHORIZATION, auth);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(request);
    }

    private static String basic(String credenciales) {
        return "Basic " + Base64.getEncoder().encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));
    }
}


