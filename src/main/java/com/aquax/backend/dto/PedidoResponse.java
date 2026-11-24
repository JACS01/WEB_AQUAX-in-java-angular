package com.aquax.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.aquax.backend.model.EstadoEntrega;
import com.aquax.backend.model.EstadoPago;
import com.aquax.backend.model.OrigenPedido;

public record PedidoResponse(
        Long id,
        String numeroPedido,
        String nombreCliente,
        String telefonoCliente,
        String direccionEntrega,
        String observaciones,
        LocalDate fechaRegistro,
        LocalDate fechaEntrega,
        OrigenPedido origen,
        EstadoEntrega estadoEntrega,
        EstadoPago estadoPago,
        long cantidadProducto,
        BigDecimal total,
        List<DetallePedidoResponse> detalles,
        Long usuarioRegistroId,
        String usuarioRegistro) {
}
