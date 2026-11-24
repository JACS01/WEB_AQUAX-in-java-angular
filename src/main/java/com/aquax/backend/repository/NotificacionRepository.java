package com.aquax.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aquax.backend.model.Notificacion;

import jakarta.persistence.LockModeType;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    @EntityGraph(attributePaths = "pedido")
    List<Notificacion> findByUsuario_IdAndLeidaFalseOrderByFechaCreacionDescIdDesc(Long usuarioId);

    @EntityGraph(attributePaths = "pedido")
    List<Notificacion> findByUsuario_IdOrderByFechaCreacionDescIdDesc(Long usuarioId);

    long countByUsuario_IdAndLeidaFalse(Long usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Notificacion n where n.id = :id and n.usuario.id = :usuarioId")
    Optional<Notificacion> buscarPropiaParaActualizar(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notificacion n set n.leida = true where n.usuario.id = :usuarioId and n.leida = false")
    int marcarTodasComoLeidas(@Param("usuarioId") Long usuarioId);
}
