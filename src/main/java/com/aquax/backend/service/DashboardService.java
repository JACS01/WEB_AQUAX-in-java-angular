package com.aquax.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aquax.backend.dto.DashboardResumenResponse;
import com.aquax.backend.model.EstadoEntrega;
import com.aquax.backend.model.EstadoPago;
import com.aquax.backend.model.OrigenPedido;
import com.aquax.backend.repository.PedidoRepository;

@Service
public class DashboardService {

    private final PedidoRepository pedidoRepository;

    public DashboardService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResumenResponse consultarResumen() {
        return pedidoRepository.obtenerResumenOperativo(
                EstadoEntrega.ENTREGADO, EstadoPago.PAGADO, EstadoPago.PENDIENTE, OrigenPedido.CLIENTE);
    }
}
