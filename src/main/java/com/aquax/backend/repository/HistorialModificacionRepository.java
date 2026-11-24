package com.aquax.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.aquax.backend.model.HistorialModificacion;

public interface HistorialModificacionRepository extends JpaRepository<HistorialModificacion, Long> {

    @EntityGraph(attributePaths = "cuentaUsuario")
    List<HistorialModificacion> findByPedido_IdOrderByFechaAscIdAsc(Long pedidoId);
}
