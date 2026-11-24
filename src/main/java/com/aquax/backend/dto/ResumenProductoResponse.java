package com.aquax.backend.dto;

public record ResumenProductoResponse(
        Long productoId,
        String nombreProducto,
        long cantidad) {
}
