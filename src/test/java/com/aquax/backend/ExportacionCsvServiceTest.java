package com.aquax.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aquax.backend.dto.DetallePedidoResponse;
import com.aquax.backend.dto.PedidoResponse;
import com.aquax.backend.model.EstadoEntrega;
import com.aquax.backend.model.EstadoPago;
import com.aquax.backend.model.OrigenPedido;
import com.aquax.backend.service.ExportacionCsvService;

class ExportacionCsvServiceTest {

    private static final String CABECERA = "\uFEFFN Pedido,Cliente,Telefono,Producto,Cantidad,Origen,Pago,Fecha\r\n";
    private final ExportacionCsvService servicio = new ExportacionCsvService();

    @Test
    void unReporteVacioConservaBomUtf8YEncabezado() {
        byte[] csv = servicio.exportarCompletados(List.of());

        assertThat(csv).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        assertThat(new String(csv, StandardCharsets.UTF_8)).isEqualTo(CABECERA);
    }

    @Test
    void conservaUnicodeComasComillasYSaltosDentroDeLasCeldas() {
        var detalle = new DetallePedidoResponse(1L, "Bidón \"Azul\", 20 L", 2,
                new BigDecimal("10.00"), new BigDecimal("20.00"));
        PedidoResponse pedido = pedido("Ana, \"Pérez\"\r\nSegunda línea", "999888777",
                List.of(detalle), LocalDate.of(2026, 11, 11));

        String csv = new String(servicio.exportarCompletados(List.of(pedido)), StandardCharsets.UTF_8);

        assertThat(csv).isEqualTo(CABECERA
                + "\"AQX-0001\",\"Ana, \"\"Pérez\"\"\r\nSegunda línea\",\"999888777\","
                + "\"Bidón \"\"Azul\"\", 20 L x2\",\"2\",\"CLIENTE\",\"PAGADO\",\"11/11/2026\"\r\n");
    }

    @Test
    void neutralizaTextosQueUnaHojaDeCalculoPuedeEjecutarComoFormulas() {
        for (String valor : List.of("=1+1", "+SUM(A1)", "-10+10", "@SUM(A1)",
                "   =1", "\u00A0=1", "\ttexto", "\rtexto", "\ntexto")) {
            PedidoResponse pedido = pedido(valor, "999888777", List.of(), null);
            String csv = new String(servicio.exportarCompletados(List.of(pedido)), StandardCharsets.UTF_8);

            assertThat(csv).as("Prefijo protegido para %s", valor)
                    .contains("\"'" + valor + "\"");
        }

        var detalle = new DetallePedidoResponse(1L, "=HYPERLINK(A1)", 2,
                new BigDecimal("10.00"), new BigDecimal("20.00"));
        String csv = new String(servicio.exportarCompletados(List.of(
                pedido("Cliente", "+51999888777", List.of(detalle), null))), StandardCharsets.UTF_8);
        assertThat(csv).contains("\"'+51999888777\"", "\"'=HYPERLINK(A1) x2\"");
    }

    @Test
    void exportaCantidadSinDetallesYFechaAusenteComoCeldasValidas() {
        String csv = new String(servicio.exportarCompletados(List.of(
                pedido("Cliente", "999888777", List.of(), null))), StandardCharsets.UTF_8);

        assertThat(csv).isEqualTo(CABECERA
                + "\"AQX-0001\",\"Cliente\",\"999888777\",\"2 unidades\",\"2\",\"CLIENTE\",\"PAGADO\",\"\"\r\n");
    }

    private PedidoResponse pedido(String cliente, String telefono, List<DetallePedidoResponse> detalles,
                                   LocalDate fechaEntrega) {
        return new PedidoResponse(1L, "AQX-0001", cliente, telefono, "Dirección", null,
                LocalDate.of(2026, 11, 10), fechaEntrega, OrigenPedido.CLIENTE,
                EstadoEntrega.ENTREGADO, EstadoPago.PAGADO, 2L, new BigDecimal("20.00"),
                detalles, null, null);
    }
}
