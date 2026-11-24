package com.aquax.backend.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aquax.backend.dto.CrearPedidoRequest;
import com.aquax.backend.dto.DetallePedidoRequest;
import com.aquax.backend.dto.DetallePedidoResponse;
import com.aquax.backend.dto.HistorialModificacionResponse;
import com.aquax.backend.dto.HistorialPagadosResponse;
import com.aquax.backend.dto.PedidoResponse;
import com.aquax.backend.dto.PaginaResponse;
import com.aquax.backend.dto.ReporteCompletadosResponse;
import com.aquax.backend.dto.ResumenProductoResponse;
import com.aquax.backend.model.EstadoEntrega;
import com.aquax.backend.model.EstadoPago;
import com.aquax.backend.model.HistorialModificacion;
import com.aquax.backend.model.OrigenPedido;
import com.aquax.backend.model.Pedido;
import com.aquax.backend.model.Producto;
import com.aquax.backend.model.Usuario;
import com.aquax.backend.repository.HistorialModificacionRepository;
import com.aquax.backend.repository.PedidoRepository;
import com.aquax.backend.repository.ProductoRepository;
import com.aquax.backend.repository.UsuarioRepository;

@Service
public class PedidoService {

    private static final Map<String, String> CAMPOS_ORDEN = Map.of(
            "id", "id", "numeroPedido", "id", "nombreCliente", "nombreCliente",
            "telefonoCliente", "telefonoCliente", "fechaRegistro", "fechaRegistro",
            "fechaEntrega", "fechaEntrega", "estadoEntrega", "estadoEntrega",
            "estadoPago", "estadoPago", "origen", "origen");

    private static final ZoneId ZONA_OPERACION = ZoneId.of("America/Lima");

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final HistorialModificacionRepository historialRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    public PedidoService(PedidoRepository pedidoRepository, ProductoRepository productoRepository,
                         HistorialModificacionRepository historialRepository,
                         UsuarioRepository usuarioRepository, NotificacionService notificacionService) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.historialRepository = historialRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public PedidoResponse registrarPedidoCliente(CrearPedidoRequest solicitud) {
        Pedido pedido = new Pedido(
                solicitud.nombreCliente(), solicitud.telefonoCliente(), solicitud.direccionEntrega(),
                solicitud.observaciones(), LocalDate.now(ZONA_OPERACION), OrigenPedido.CLIENTE);
        guardarPedido(solicitud, pedido);
        notificacionService.avisarPedidoCliente(pedido);
        return aRespuesta(pedido);
    }

    @Transactional
    public PedidoResponse registrarPedidoPersonal(CrearPedidoRequest solicitud, String nombreUsuario) {
        Usuario usuario = consultarUsuario(nombreUsuario);
        Instant fecha = Instant.now();
        Pedido pedido = new Pedido(
                solicitud.nombreCliente(), solicitud.telefonoCliente(), solicitud.direccionEntrega(),
                solicitud.observaciones(), LocalDate.ofInstant(fecha, ZONA_OPERACION), usuario);
        guardarPedido(solicitud, pedido);
        historialRepository.save(new HistorialModificacion(
                pedido, usuario, "creacion", "N/A", OrigenPedido.TRABAJADOR.name(), fecha));
        return aRespuesta(pedido);
    }

    private void guardarPedido(CrearPedidoRequest solicitud, Pedido pedido) {
        List<Long> idsProducto = solicitud.detalles().stream()
                .map(DetallePedidoRequest::productoId)
                .distinct()
                .toList();
        Map<Long, Producto> productos = productoRepository.findAllById(idsProducto).stream()
                .collect(Collectors.toMap(Producto::getId, Function.identity()));

        for (DetallePedidoRequest detalle : solicitud.detalles()) {
            Producto producto = productos.get(detalle.productoId());
            if (producto == null) {
                throw new ProductoNoEncontradoException(detalle.productoId());
            }
            pedido.agregarDetalle(producto, detalle.cantidad());
        }

        pedidoRepository.save(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarParaPersonal(boolean completados) {
        List<Pedido> pedidos = completados
                ? pedidoRepository.findByEstadoEntregaAndEstadoPagoOrderByFechaEntregaDescIdDesc(
                        EstadoEntrega.ENTREGADO, EstadoPago.PAGADO)
                : pedidoRepository.findByEstadoEntregaNotOrEstadoPagoNotOrderByFechaRegistroDescIdDesc(
                        EstadoEntrega.ENTREGADO, EstadoPago.PAGADO);
        return pedidos.stream().map(this::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PedidoResponse> listarPagina(boolean completados, String texto, Pageable pagina) {
        Pageable segura = Paginacion.segura(pagina, CAMPOS_ORDEN);
        Page<Long> ids = pedidoRepository.buscarPaginaIds(
                completados, EstadoEntrega.ENTREGADO, EstadoPago.PAGADO,
                Paginacion.patron(texto), idDesdeTexto(texto), segura);
        Map<Long, Pedido> pedidos = new HashMap<>();
        if (ids.hasContent()) {
            pedidoRepository.buscarConDetalles(ids.getContent())
                    .forEach(pedido -> pedidos.put(pedido.getId(), pedido));
        }
        return PaginaResponse.de(ids.map(id -> aRespuesta(pedidos.get(id))));
    }

    @Transactional(readOnly = true)
    public HistorialPagadosResponse consultarPagados() {
        List<PedidoResponse> pedidos = pedidoRepository
                .findByEstadoPagoOrderByFechaRegistroDescIdDesc(EstadoPago.PAGADO)
                .stream().map(this::aRespuesta).toList();
        BigDecimal montoTotal = pedidos.stream().map(PedidoResponse::total)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        return new HistorialPagadosResponse(pedidos.size(), montoTotal, pedidos);
    }

    @Transactional(readOnly = true)
    public ReporteCompletadosResponse consultarCompletados(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha desde debe ser anterior o igual a la fecha hasta.");
        }
        List<PedidoResponse> pedidos = pedidoRepository
                .buscarCompletadosPorFecha(EstadoEntrega.ENTREGADO, EstadoPago.PAGADO, desde, hasta)
                .stream().map(this::aRespuesta).toList();
        Map<Long, Long> cantidades = new HashMap<>();
        for (PedidoResponse pedido : pedidos) {
            for (DetallePedidoResponse detalle : pedido.detalles()) {
                cantidades.merge(detalle.productoId(), (long) detalle.cantidad(), Long::sum);
            }
        }
        List<ResumenProductoResponse> productos = productoRepository.findAll(Sort.by("id")).stream()
                .map(producto -> new ResumenProductoResponse(
                        producto.getId(), producto.getNombre(), cantidades.getOrDefault(producto.getId(), 0L)))
                .toList();
        return new ReporteCompletadosResponse(desde, hasta, pedidos.size(), productos, pedidos);
    }

    @Transactional(readOnly = true)
    public List<HistorialModificacionResponse> consultarHistorial(Long pedidoId) {
        if (!pedidoRepository.existsById(pedidoId)) {
            throw new PedidoNoEncontradoException(pedidoId);
        }
        return historialRepository.findByPedido_IdOrderByFechaAscIdAsc(pedidoId).stream()
                .map(cambio -> new HistorialModificacionResponse(
                        cambio.getId(), pedidoId,
                        cambio.getCuentaUsuario() == null ? null : cambio.getCuentaUsuario().getId(),
                        cambio.getUsuario(), cambio.getCampo(), cambio.getValorAnterior(),
                        cambio.getValorNuevo(), cambio.getFecha().atZone(ZONA_OPERACION).toOffsetDateTime()))
                .toList();
    }

    @Transactional
    public PedidoResponse cambiarEntrega(Long id, EstadoEntrega nuevoEstado, String usuario) {
        Pedido pedido = buscarParaActualizar(id);
        EstadoEntrega anterior = pedido.getEstadoEntrega();
        if (anterior != nuevoEstado) {
            Instant fecha = Instant.now();
            pedido.cambiarEstadoEntrega(nuevoEstado, LocalDate.ofInstant(fecha, ZONA_OPERACION));
            historialRepository.save(new HistorialModificacion(
                    pedido, consultarUsuario(usuario), "estadoEntrega", anterior.name(), nuevoEstado.name(), fecha));
        }
        return aRespuesta(pedido);
    }

    @Transactional
    public PedidoResponse cambiarPago(Long id, EstadoPago nuevoEstado, String usuario) {
        Pedido pedido = buscarParaActualizar(id);
        EstadoPago anterior = pedido.getEstadoPago();
        if (anterior != nuevoEstado) {
            pedido.cambiarEstadoPago(nuevoEstado);
            historialRepository.save(new HistorialModificacion(
                    pedido, consultarUsuario(usuario), "estadoPago", anterior.name(), nuevoEstado.name(), Instant.now()));
        }
        return aRespuesta(pedido);
    }

    private Pedido buscarParaActualizar(Long id) {
        return pedidoRepository.buscarParaActualizar(id)
                .orElseThrow(() -> new PedidoNoEncontradoException(id));
    }

    private Usuario consultarUsuario(String nombreUsuario) {
        return usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);
    }

    private Long idDesdeTexto(String texto) {
        String codigo = texto == null ? "" : texto.trim().toUpperCase(Locale.ROOT);
        if (codigo.startsWith("AQX-")) {
            codigo = codigo.substring(4);
        }
        if (!codigo.matches("[0-9]+")) {
            return null;
        }
        try {
            return Long.valueOf(codigo);
        } catch (NumberFormatException error) {
            return null;
        }
    }

    private PedidoResponse aRespuesta(Pedido pedido) {
        Usuario usuarioRegistro = pedido.getUsuarioRegistro();
        List<DetallePedidoResponse> detalles = pedido.getDetalles().stream()
                .map(detalle -> new DetallePedidoResponse(
                        detalle.getProducto().getId(), detalle.getProducto().getNombre(),
                        detalle.getCantidad(), detalle.getPrecioUnitario(), detalle.getSubtotal()))
                .toList();
        return new PedidoResponse(
                pedido.getId(), pedido.getNumeroPedido(), pedido.getNombreCliente(),
                pedido.getTelefonoCliente(), pedido.getDireccionEntrega(), pedido.getObservaciones(),
                pedido.getFechaRegistro(), pedido.getFechaEntrega(), pedido.getOrigen(),
                pedido.getEstadoEntrega(), pedido.getEstadoPago(), pedido.getCantidadProducto(),
                pedido.getTotal(), detalles,
                usuarioRegistro == null ? null : usuarioRegistro.getId(),
                usuarioRegistro == null ? null : usuarioRegistro.getNombreUsuario());
    }
}
