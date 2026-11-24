package com.aquax.backend.dto;

import java.math.BigDecimal;

public record DetallePedidoResponse(
        Long productoId,
        String nombreProducto,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal) {
}
