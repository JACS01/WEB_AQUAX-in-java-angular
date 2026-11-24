package com.aquax.backend.dto;

import com.aquax.backend.model.EstadoEntrega;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoEntregaRequest(
        @NotNull(message = "El estado de entrega es obligatorio.") EstadoEntrega estadoEntrega) {
}
