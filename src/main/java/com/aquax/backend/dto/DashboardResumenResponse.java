package com.aquax.backend.dto;

public record DashboardResumenResponse(
        long totalPedidos,
        long pendientes,
        long pagados,
        long deClientes) {
}
