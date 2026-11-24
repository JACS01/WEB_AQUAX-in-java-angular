package com.aquax.backend.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearPedidoRequest(
        @NotBlank(message = "El nombre del cliente es obligatorio.")
        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres.")
        String nombreCliente,

        @NotBlank(message = "El teléfono es obligatorio.")
        @Size(max = 15, message = "El teléfono no puede exceder 15 caracteres.")
        String telefonoCliente,

        @NotBlank(message = "La dirección de entrega es obligatoria.")
        @Size(max = 300, message = "La dirección no puede exceder 300 caracteres.")
        String direccionEntrega,

        @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres.")
        String observaciones,

        @NotEmpty(message = "El pedido debe incluir al menos un producto.")
        List<@NotNull(message = "El detalle del pedido es obligatorio.") @Valid DetallePedidoRequest> detalles) {
}
