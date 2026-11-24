package com.aquax.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aquax.backend.dto.DashboardResumenResponse;
import com.aquax.backend.model.EstadoEntrega;
import com.aquax.backend.model.EstadoPago;
import com.aquax.backend.model.OrigenPedido;
import com.aquax.backend.model.Pedido;

import jakarta.persistence.LockModeType;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    String FILTRO_PAGINA = """
            from Pedido pedido
            where ((:completados = true and pedido.estadoEntrega = :entregado and pedido.estadoPago = :pagado)
                or (:completados = false and (pedido.estadoEntrega <> :entregado or pedido.estadoPago <> :pagado)))
              and (lower(pedido.nombreCliente) like :patron escape '!'
                or lower(pedido.telefonoCliente) like :patron escape '!'
                or pedido.id = :idPedido)
            """;

    @Query(value = "select pedido.id " + FILTRO_PAGINA, countQuery = "select count(pedido) " + FILTRO_PAGINA)
    Page<Long> buscarPaginaIds(
            @Param("completados") boolean completados,
            @Param("entregado") EstadoEntrega entregado,
            @Param("pagado") EstadoPago pagado,
            @Param("patron") String patron,
            @Param("idPedido") Long idPedido,
            Pageable pagina);

    @EntityGraph(attributePaths = {"detalles", "detalles.producto", "usuarioRegistro"})
    @Query("select pedido from Pedido pedido where pedido.id in :ids")
    List<Pedido> buscarConDetalles(@Param("ids") List<Long> ids);

    @Query("""
            select new com.aquax.backend.dto.DashboardResumenResponse(
                count(pedido),
                coalesce(sum(case when pedido.estadoPago = :pendiente then 1L else 0L end), 0L),
                coalesce(sum(case when pedido.estadoPago = :pagado then 1L else 0L end), 0L),
                coalesce(sum(case when pedido.origen = :cliente then 1L else 0L end), 0L))
            from Pedido pedido
            where pedido.estadoEntrega <> :entregado or pedido.estadoPago <> :pagado
            """)
    DashboardResumenResponse obtenerResumenOperativo(
            @Param("entregado") EstadoEntrega entregado,
            @Param("pagado") EstadoPago pagado,
            @Param("pendiente") EstadoPago pendiente,
            @Param("cliente") OrigenPedido cliente);

    @EntityGraph(attributePaths = {"detalles", "detalles.producto", "usuarioRegistro"})
    List<Pedido> findByEstadoEntregaNotOrEstadoPagoNotOrderByFechaRegistroDescIdDesc(
            EstadoEntrega estadoEntrega, EstadoPago estadoPago);

    @EntityGraph(attributePaths = {"detalles", "detalles.producto", "usuarioRegistro"})
    List<Pedido> findByEstadoEntregaAndEstadoPagoOrderByFechaEntregaDescIdDesc(
            EstadoEntrega estadoEntrega, EstadoPago estadoPago);

    @EntityGraph(attributePaths = {"detalles", "detalles.producto", "usuarioRegistro"})
    List<Pedido> findByEstadoPagoOrderByFechaRegistroDescIdDesc(EstadoPago estadoPago);

    @EntityGraph(attributePaths = {"detalles", "detalles.producto", "usuarioRegistro"})
    @Query("""
            select pedido from Pedido pedido
            where pedido.estadoEntrega = :entregado
              and pedido.estadoPago = :pagado
              and (:desde is null or pedido.fechaEntrega >= :desde)
              and (:hasta is null or pedido.fechaEntrega <= :hasta)
            order by pedido.fechaEntrega desc nulls last, pedido.id desc
            """)
    List<Pedido> buscarCompletadosPorFecha(
            @Param("entregado") EstadoEntrega entregado,
            @Param("pagado") EstadoPago pagado,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pedido from Pedido pedido where pedido.id = :id")
    Optional<Pedido> buscarParaActualizar(@Param("id") Long id);
}
