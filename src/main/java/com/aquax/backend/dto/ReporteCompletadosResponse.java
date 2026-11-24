package com.aquax.backend.dto;

import java.time.LocalDate;
import java.util.List;

public record ReporteCompletadosResponse(
        LocalDate desde,
        LocalDate hasta,
        long totalPedidos,
        List<ResumenProductoResponse> productos,
        List<PedidoResponse> pedidos) {
}
