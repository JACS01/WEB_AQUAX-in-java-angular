package com.aquax.backend.service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.aquax.backend.dto.NotificacionResponse;
import com.aquax.backend.model.Notificacion;
import com.aquax.backend.model.Pedido;
import com.aquax.backend.model.RolUsuario;
import com.aquax.backend.model.Usuario;
import com.aquax.backend.repository.NotificacionRepository;
import com.aquax.backend.repository.UsuarioRepository;

@Service
public class NotificacionService {

    private static final ZoneId ZONA_OPERACION = ZoneId.of("America/Lima");

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    public NotificacionService(NotificacionRepository notificacionRepository, UsuarioRepository usuarioRepository) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void avisarPedidoCliente(Pedido pedido) {
        Instant fecha = Instant.now();
        List<Notificacion> notificaciones = usuarioRepository
                .findByActivoTrueAndRolIn(List.of(RolUsuario.ADMINISTRADOR, RolUsuario.VENDEDOR)).stream()
                .map(usuario -> new Notificacion(pedido, usuario, fecha))
                .toList();
        notificacionRepository.saveAll(notificaciones);
    }

    @Transactional(readOnly = true)
    public List<NotificacionResponse> listar(String nombreUsuario, boolean soloNoLeidas) {
        Long usuarioId = consultarUsuario(nombreUsuario).getId();
        List<Notificacion> notificaciones = soloNoLeidas
                ? notificacionRepository.findByUsuario_IdAndLeidaFalseOrderByFechaCreacionDescIdDesc(usuarioId)
                : notificacionRepository.findByUsuario_IdOrderByFechaCreacionDescIdDesc(usuarioId);
        return notificaciones.stream().map(this::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(String nombreUsuario) {
        return notificacionRepository.countByUsuario_IdAndLeidaFalse(consultarUsuario(nombreUsuario).getId());
    }

    @Transactional
    public NotificacionResponse marcarComoLeida(Long id, String nombreUsuario) {
        Long usuarioId = consultarUsuario(nombreUsuario).getId();
        Notificacion notificacion = notificacionRepository.buscarPropiaParaActualizar(id, usuarioId)
                .orElseThrow(NotificacionNoEncontradaException::new);
        notificacion.marcarComoLeida();
        return aRespuesta(notificacion);
    }

    @Transactional
    public int marcarTodasComoLeidas(String nombreUsuario) {
        return notificacionRepository.marcarTodasComoLeidas(consultarUsuario(nombreUsuario).getId());
    }

    private Usuario consultarUsuario(String nombreUsuario) {
        return usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);
    }

    private NotificacionResponse aRespuesta(Notificacion notificacion) {
        return new NotificacionResponse(
                notificacion.getId(), notificacion.getPedido().getId(),
                notificacion.getPedido().getNumeroPedido(), notificacion.getTitulo(),
                notificacion.getMensaje(), notificacion.getTipo(), notificacion.isLeida(),
                notificacion.getFechaCreacion().atZone(ZONA_OPERACION).toOffsetDateTime());
    }
}
