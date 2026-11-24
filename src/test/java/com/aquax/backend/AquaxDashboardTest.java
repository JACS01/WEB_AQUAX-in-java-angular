package com.aquax.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {"aquax.personal.usuario=personal", "aquax.personal.contrasena=1234", "aquax.jwt.secret-base64=", "aquax.jwt.expiration-seconds=3600", "aquax.cors.allowed-origins=http://localhost:4200"})
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("sqlite")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class AquaxDashboardTest {

    private static final Path DB = Path.of("target", "dashboard-" + UUID.randomUUID() + ".db").toAbsolutePath();
    private static final String RUTA = "/api/personal/dashboard/resumen";
    private static final String ADMIN = basic("personal:1234");
    private static final String VENDEDOR = basic("vendedor1:ClaveVendedor2026!");
    private static final String PEDIDO = """
            {"nombreCliente":"Cliente dashboard","telefonoCliente":"999888777","direccionEntrega":"Dirección dashboard",
            "detalles":[{"productoId":1,"cantidad":2},{"productoId":2,"cantidad":1}]}
            """;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> "jdbc:sqlite:" + DB);
    }

    @Test
    void resumenEsConsistenteConPedidosOperativosYEstadosIndependientes() throws Exception {
        solicitud("GET", RUTA, null, null).andExpect(status().isUnauthorized());
        solicitud("GET", RUTA, basic("personal:incorrecta"), null).andExpect(status().isUnauthorized());
        verificarResumen(ADMIN, 0, 0, 0, 0);
        solicitud("POST", "/api/admin/usuarios", ADMIN, """
                {"nombreUsuario":"vendedor1","nombre":"Ana","apellido":"Pérez","correo":"ana@example.com",
                "contrasena":"ClaveVendedor2026!","rol":"VENDEDOR"}
                """).andExpect(status().isCreated());
        verificarResumen(VENDEDOR, 0, 0, 0, 0);

        long completadoCliente = 0;
        for (String origen : new String[] {"CLIENTE", "TRABAJADOR"}) {
            for (String entrega : new String[] {"NUEVO", "EN_CAMINO", "ENTREGADO"}) {
                for (String pago : new String[] {"PENDIENTE", "PAGADO"}) {
                    String ruta = origen.equals("CLIENTE") ? "/api/pedidos" : "/api/personal/pedidos";
                    solicitud("POST", ruta, origen.equals("CLIENTE") ? null : ADMIN, PEDIDO)
                            .andExpect(status().isCreated());
                    long id = jdbc.queryForObject("select max(id) from pedidos", Long.class);
                    if (!entrega.equals("NUEVO")) {
                        solicitud("PATCH", "/api/personal/pedidos/" + id + "/entrega", ADMIN,
                                "{\"estadoEntrega\":\"" + entrega + "\"}").andExpect(status().isOk());
                    }
                    if (pago.equals("PAGADO")) {
                        solicitud("PATCH", "/api/personal/pedidos/" + id + "/pago", ADMIN,
                                "{\"estadoPago\":\"PAGADO\"}").andExpect(status().isOk());
                    }
                    if (origen.equals("CLIENTE") && entrega.equals("ENTREGADO") && pago.equals("PAGADO")) {
                        completadoCliente = id;
                    }
                }
            }
        }

        assertThat(jdbc.queryForObject("select count(*) from pedidos", Long.class)).isEqualTo(12L);
        assertThat(jdbc.queryForObject("select count(*) from detalles_pedido", Long.class)).isEqualTo(24L);
        verificarResumen(ADMIN, 10, 6, 4, 5);
        verificarResumen(VENDEDOR, 10, 6, 4, 5);
        solicitud("GET", "/api/personal/pedidos", VENDEDOR, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(10));
        solicitud("GET", "/api/personal/pedidos?completados=true", ADMIN, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));

        solicitud("PATCH", "/api/personal/pedidos/" + completadoCliente + "/entrega", VENDEDOR,
                "{\"estadoEntrega\":\"EN_CAMINO\"}").andExpect(status().isOk());
        verificarResumen(ADMIN, 11, 6, 5, 6);
        solicitud("PATCH", "/api/personal/pedidos/" + completadoCliente + "/pago", VENDEDOR,
                "{\"estadoPago\":\"PENDIENTE\"}").andExpect(status().isOk());
        verificarResumen(ADMIN, 11, 7, 4, 6);
        solicitud("PATCH", "/api/personal/pedidos/" + completadoCliente + "/entrega", VENDEDOR,
                "{\"estadoEntrega\":\"ENTREGADO\"}").andExpect(status().isOk());
        verificarResumen(ADMIN, 11, 7, 4, 6);
        solicitud("PATCH", "/api/personal/pedidos/" + completadoCliente + "/pago", VENDEDOR,
                "{\"estadoPago\":\"PAGADO\"}").andExpect(status().isOk());
        verificarResumen(ADMIN, 10, 6, 4, 5);

        long vendedorId = jdbc.queryForObject(
                "select id from usuarios where nombre_usuario='vendedor1'", Long.class);
        solicitud("PATCH", "/api/admin/usuarios/" + vendedorId + "/activo", ADMIN,
                "{\"activo\":false}").andExpect(status().isOk());
        solicitud("GET", RUTA, VENDEDOR, null).andExpect(status().isUnauthorized());
        verificarResumen(ADMIN, 10, 6, 4, 5);
    }

    private void verificarResumen(String auth, int total, int pendientes, int pagados, int deClientes)
            throws Exception {
        String respuesta = solicitud("GET", RUTA, auth, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(total))
                .andExpect(jsonPath("$.pendientes").value(pendientes))
                .andExpect(jsonPath("$.pagados").value(pagados))
                .andExpect(jsonPath("$.deClientes").value(deClientes))
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$.detalles").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertThat(respuesta).doesNotContain("contrasena", "nombreCliente", "telefonoCliente");
    }

    private ResultActions solicitud(String metodo, String ruta, String auth, String json) throws Exception {
        var request = switch (metodo) {
            case "GET" -> get(ruta);
            case "POST" -> post(ruta);
            case "PATCH" -> patch(ruta);
            default -> throw new IllegalArgumentException(metodo);
        };
        if (auth != null) request.header(HttpHeaders.AUTHORIZATION, auth);
        if (json != null) request.contentType(MediaType.APPLICATION_JSON).content(json);
        return mvc.perform(request);
    }

    private static String basic(String credenciales) {
        return "Basic " + Base64.getEncoder().encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));
    }
}


