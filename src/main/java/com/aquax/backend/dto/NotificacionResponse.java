package com.aquax.backend.dto;

import java.time.OffsetDateTime;

public record NotificacionResponse(
        Long id,
        Long pedidoId,
        String numeroPedido,
        String titulo,
        String mensaje,
        String tipo,
        boolean leida,
        OffsetDateTime fechaCreacion) {
}
