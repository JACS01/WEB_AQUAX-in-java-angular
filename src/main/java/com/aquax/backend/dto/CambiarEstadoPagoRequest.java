package com.aquax.backend.dto;

import com.aquax.backend.model.EstadoPago;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoPagoRequest(
        @NotNull(message = "El estado de pago es obligatorio.") EstadoPago estadoPago) {
}
