package com.aquax.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.aquax.backend.model.EstadoEntrega;
import com.aquax.backend.model.EstadoPago;
import com.aquax.backend.model.OrigenPedido;
import com.aquax.backend.model.Pedido;
import com.aquax.backend.model.Producto;
import com.aquax.backend.repository.PedidoRepository;
import com.aquax.backend.repository.ProductoRepository;
import com.aquax.backend.repository.UsuarioRepository;

@SpringBootTest(properties = {"aquax.personal.usuario=personal", "aquax.personal.contrasena=1234", "aquax.jwt.secret-base64=", "aquax.jwt.expiration-seconds=3600", "aquax.cors.allowed-origins=http://localhost:4200"})
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("sqlite")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class AquaxCompletadosTest {

    private static final Path DB = Path.of("target", "completados-" + UUID.randomUUID() + ".db").toAbsolutePath();
    private static final String RUTA = "/api/personal/reportes/completados";
    private static final String ADMIN = basic("personal:1234");
    private static final String VENDEDOR = basic("vendedor1:ClaveVendedor2026!");
    private static final String RANGO = "?desde=2026-11-01&hasta=2026-11-10";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PedidoRepository pedidoRepository;
    @Autowired ProductoRepository productoRepository;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> "jdbc:sqlite:" + DB);
    }

    @Test
    void reporteYExportacionCompartenFiltrosDatosYPermisos() throws Exception {
        for (String ruta : List.of(RUTA, RUTA + "/csv")) {
            obtener(ruta, null).andExpect(status().isUnauthorized());
            obtener(ruta, basic("personal:incorrecta")).andExpect(status().isUnauthorized());
        }
        obtener(RUTA, ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(0))
                .andExpect(jsonPath("$.pedidos.length()").value(0))
                .andExpect(jsonPath("$.productos.length()").value(3))
                .andExpect(jsonPath("$.productos[0].cantidad").value(0));
        var empty = obtener(RUTA + "/csv", ADMIN).andExpect(status().isOk()).andReturn().getResponse();
        assertThat(empty.getContentAsByteArray()).startsWith((byte)0xEF, (byte)0xBB, (byte)0xBF);
        assertThat(parseCsv(empty.getContentAsString(StandardCharsets.UTF_8))).hasSize(1);

        mvc.perform(post("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"nombreUsuario":"vendedor1","nombre":"Ana","apellido":"Perez","correo":"ana@example.com",
                "contrasena":"ClaveVendedor2026!","rol":"VENDEDOR"}
                """)).andExpect(status().isCreated());
        obtener(RUTA, VENDEDOR).andExpect(status().isOk());

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        List<Long> ids = transaction.execute(status -> {
            var p1 = productoRepository.findById(1L).orElseThrow();
            var p2 = productoRepository.findById(2L).orElseThrow();
            List<Long> result = new ArrayList<>();
            result.add(crear("Antes", LocalDate.of(2026, 10, 31), false, true, p1, 1, null, 0));
            result.add(crear("Desde", LocalDate.of(2026, 11, 1), false, true, p1, 2, p2, 1));
            result.add(crear("Medio", LocalDate.of(2026, 11, 5), true, true, p1, 3, p2, 2));
            result.add(crear("Ana, \"Pérez\"\nCliente", LocalDate.of(2026, 11, 10), false, true, p1, 1, p2, 4));
            result.add(crear("  =2+2", LocalDate.of(2026, 11, 10), true, true, p1, 2, p2, 1));
            result.add(crear("Después", LocalDate.of(2026, 11, 11), false, true, p2, 3, null, 0));
            result.add(crear("Sin fecha antigua", LocalDate.of(2026, 11, 5), false, true, p1, 4, null, 0));
            result.add(crear("Entregado sin pago", LocalDate.of(2026, 11, 5), false, false, p1, 100, null, 0));
            Pedido soloPagado = new Pedido("Pagado sin entregar", "999888777", "Direccion", null,
                    LocalDate.of(2026, 11, 1), OrigenPedido.CLIENTE);
            soloPagado.agregarDetalle(p1, 100);
            soloPagado.cambiarEstadoPago(EstadoPago.PAGADO);
            result.add(pedidoRepository.saveAndFlush(soloPagado).getId());
            return result;
        });
        jdbc.update("update pedidos set fecha_entrega = null where id = ?", ids.get(6));

        obtener(RUTA + RANGO, ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.desde").value("2026-11-01"))
                .andExpect(jsonPath("$.hasta").value("2026-11-10"))
                .andExpect(jsonPath("$.totalPedidos").value(4))
                .andExpect(jsonPath("$.pedidos.length()").value(4))
                .andExpect(jsonPath("$.pedidos[0].id").value(ids.get(4)))
                .andExpect(jsonPath("$.pedidos[1].id").value(ids.get(3)))
                .andExpect(jsonPath("$.pedidos[2].id").value(ids.get(2)))
                .andExpect(jsonPath("$.pedidos[3].id").value(ids.get(1)))
                .andExpect(jsonPath("$.productos[0].productoId").value(1))
                .andExpect(jsonPath("$.productos[0].cantidad").value(8))
                .andExpect(jsonPath("$.productos[1].productoId").value(2))
                .andExpect(jsonPath("$.productos[1].cantidad").value(8))
                .andExpect(jsonPath("$.productos[2].productoId").value(3))
                .andExpect(jsonPath("$.productos[2].cantidad").value(0));
        obtener(RUTA + RANGO, VENDEDOR).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(4));

        obtener(RUTA, ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(7))
                .andExpect(jsonPath("$.pedidos[0].id").value(ids.get(5)))
                .andExpect(jsonPath("$.pedidos[6].id").value(ids.get(6)))
                .andExpect(jsonPath("$.productos[0].cantidad").value(13))
                .andExpect(jsonPath("$.productos[1].cantidad").value(11));
        obtener(RUTA + "?desde=2026-11-01", ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(5));
        obtener(RUTA + "?hasta=2026-11-10", ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(5));
        obtener(RUTA + "?desde=2026-11-10&hasta=2026-11-10", ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(2))
                .andExpect(jsonPath("$.productos[0].cantidad").value(3))
                .andExpect(jsonPath("$.productos[1].cantidad").value(5));
        obtener(RUTA + "?desde=2030-01-01", ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(0))
                .andExpect(jsonPath("$.productos[0].cantidad").value(0));

        for (String sufijo : List.of("", "/csv")) {
            obtener(RUTA + sufijo + "?desde=2026-11-11&hasta=2026-11-01", ADMIN)
                    .andExpect(status().isBadRequest());
            obtener(RUTA + sufijo + "?desde=fecha-invalida", ADMIN)
                    .andExpect(status().isBadRequest());
            obtener(RUTA + sufijo + "?hasta=2026-02-30", ADMIN)
                    .andExpect(status().isBadRequest());
        }

        var csvResponse = obtener(RUTA + "/csv" + RANGO, ADMIN).andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("attachment")))
                .andReturn().getResponse();
        assertThat(csvResponse.getContentType()).startsWith("text/csv");
        assertThat(csvResponse.getContentAsByteArray()).startsWith((byte)0xEF, (byte)0xBB, (byte)0xBF);
        String csv = csvResponse.getContentAsString(StandardCharsets.UTF_8);
        List<List<String>> rows = parseCsv(csv);
        assertThat(rows).hasSize(5);
        assertThat(rows.get(0)).containsExactly("N Pedido", "Cliente", "Telefono", "Producto",
                "Cantidad", "Origen", "Pago", "Fecha");
        assertThat(rows.subList(1, rows.size()).stream().map(row -> row.get(0)).toList())
                .containsExactly(numero(ids.get(4)), numero(ids.get(3)), numero(ids.get(2)), numero(ids.get(1)));
        assertThat(rows.get(1).get(1)).startsWith("'").contains("=2+2");
        assertThat(rows.get(2).get(1)).isEqualTo("Ana, \"Pérez\"\nCliente");
        assertThat(rows.get(2).get(2)).startsWith("'").contains("+51999888777");
        assertThat(rows.get(2).get(3)).contains("Bidón 20L x1", "Paquete 15 x 500 ml x4");
        assertThat(rows.get(2).get(7)).isEqualTo("10/11/2026");
        assertThat(rows.subList(1, rows.size())).allSatisfy(row -> assertThat(row).hasSize(8));
        assertThat(csv).contains("\r\n");
        obtener(RUTA + "/csv" + RANGO, VENDEDOR).andExpect(status().isOk());

        for (String nombre : List.of("=SUM(1,2)", "+1", "-2", "@SUM(1,2)", "\ttexto", "\ntexto", "  =3")) {
            long id = transaction.execute(status -> crear(nombre, LocalDate.of(2026, 11, 20), false, true,
                    productoRepository.findById(1L).orElseThrow(), 1, null, 0));
            var response = obtener(RUTA + "/csv?desde=2026-11-20&hasta=2026-11-20", ADMIN)
                    .andExpect(status().isOk()).andReturn().getResponse();
            List<List<String>> formulaRows = parseCsv(response.getContentAsString(StandardCharsets.UTF_8));
            assertThat(formulaRows.get(1).get(1)).startsWith("'").contains(nombre);
        }

        obtener("/api/personal/dashboard/resumen", ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(2));
        obtener("/api/personal/pedidos", ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        jdbc.update("update usuarios set activo = false where nombre_usuario = 'vendedor1'");
        obtener(RUTA, VENDEDOR).andExpect(status().isUnauthorized());
        obtener(RUTA + "/csv", VENDEDOR).andExpect(status().isUnauthorized());
    }

    private Long crear(String nombre, LocalDate entrega, boolean trabajador, boolean pagado,
                       Producto p1, int cantidad1, Producto p2, int cantidad2) {
        Pedido pedido = trabajador
                ? new Pedido(nombre, "+51999888777", "Direccion", null, entrega.minusDays(3),
                        usuarioRepository.findByNombreUsuario("vendedor1").orElseThrow())
                : new Pedido(nombre, "+51999888777", "Direccion", null, entrega.minusDays(3), OrigenPedido.CLIENTE);
        pedido.agregarDetalle(p1, cantidad1);
        if (p2 != null) {
            pedido.agregarDetalle(p2, cantidad2);
        }
        pedido.cambiarEstadoEntrega(EstadoEntrega.ENTREGADO, entrega);
        if (pagado) {
            pedido.cambiarEstadoPago(EstadoPago.PAGADO);
        }
        return pedidoRepository.saveAndFlush(pedido).getId();
    }

    private ResultActions obtener(String ruta, String auth) throws Exception {
        var request = get(ruta);
        if (auth != null) {
            request.header(HttpHeaders.AUTHORIZATION, auth);
        }
        return mvc.perform(request);
    }

    private static String numero(long id) {
        return "AQX-%04d".formatted(id);
    }

    private static String basic(String credenciales) {
        return "Basic " + Base64.getEncoder().encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));
    }

    private static List<List<String>> parseCsv(String csv) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = csv.charAt(0) == '\uFEFF' ? 1 : 0; i < csv.length(); i++) {
            char c = csv.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                row.add(cell.toString());
                cell.setLength(0);
            } else if ((c == '\r' || c == '\n') && !quoted) {
                row.add(cell.toString());
                rows.add(List.copyOf(row));
                row.clear();
                cell.setLength(0);
                if (c == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') {
                    i++;
                }
            } else {
                cell.append(c);
            }
        }
        if (!row.isEmpty() || !cell.isEmpty()) {
            row.add(cell.toString());
            rows.add(List.copyOf(row));
        }
        return rows;
    }
}


