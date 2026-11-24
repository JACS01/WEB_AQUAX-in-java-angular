package com.aquax.backend.dto;

import java.time.OffsetDateTime;

public record HistorialModificacionResponse(
        Long id,
        Long pedidoId,
        Long usuarioId,
        String usuario,
        String campo,
        String valorAnterior,
        String valorNuevo,
        OffsetDateTime fecha) {
}
