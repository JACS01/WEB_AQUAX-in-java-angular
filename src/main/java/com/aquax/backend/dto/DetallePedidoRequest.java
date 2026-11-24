package com.aquax.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DetallePedidoRequest(
        @NotNull(message = "El identificador del producto es obligatorio.")
        @Positive(message = "El identificador del producto debe ser mayor a cero.")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria.")
        @Positive(message = "La cantidad debe ser mayor a cero.")
        Integer cantidad) {
}
