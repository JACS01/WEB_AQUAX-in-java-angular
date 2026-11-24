package com.aquax.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record HistorialPagadosResponse(
        long totalPagados,
        BigDecimal montoTotal,
        List<PedidoResponse> pedidos) {
}
