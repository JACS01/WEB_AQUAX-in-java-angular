package com.aquax.backend.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aquax.backend.dto.ActualizarPreferenciasRequest;
import com.aquax.backend.dto.PreferenciasResponse;
import com.aquax.backend.model.ConfiguracionUsuario;
import com.aquax.backend.model.Usuario;
import com.aquax.backend.repository.ConfiguracionUsuarioRepository;
import com.aquax.backend.repository.UsuarioRepository;

@Service
public class ConfiguracionUsuarioService {

    private static final Set<Integer> NIVELES_ZOOM = Set.of(80, 90, 100, 110, 120, 130, 150);

    private final ConfiguracionUsuarioRepository configuracionRepository;
    private final UsuarioRepository usuarioRepository;

    public ConfiguracionUsuarioService(
            ConfiguracionUsuarioRepository configuracionRepository, UsuarioRepository usuarioRepository) {
        this.configuracionRepository = configuracionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public PreferenciasResponse consultar(String nombreUsuario) {
        Usuario usuario = usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);
        ConfiguracionUsuario configuracion = configuracionRepository.findByUsuario_Id(usuario.getId())
                .orElseGet(() -> new ConfiguracionUsuario(usuario));
        return aRespuesta(configuracion);
    }

    @Transactional
    public PreferenciasResponse actualizar(String nombreUsuario, ActualizarPreferenciasRequest solicitud) {
        if (!NIVELES_ZOOM.contains(solicitud.nivelZoom())) {
            throw new IllegalArgumentException("El zoom debe ser 80, 90, 100, 110, 120, 130 o 150.");
        }
        // El bloqueo de la cuenta también serializa la creación inicial de sus preferencias.
        Usuario usuario = usuarioRepository.buscarPorNombreParaActualizar(nombreUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);
        ConfiguracionUsuario configuracion = configuracionRepository.findByUsuario_Id(usuario.getId())
                .orElseGet(() -> new ConfiguracionUsuario(usuario));
        configuracion.actualizar(solicitud.idioma(), solicitud.nivelZoom(),
                solicitud.filtroColor(), solicitud.filtroColorActivo());
        return aRespuesta(configuracionRepository.saveAndFlush(configuracion));
    }

    private PreferenciasResponse aRespuesta(ConfiguracionUsuario configuracion) {
        return new PreferenciasResponse(
                configuracion.getUsuario().getId(), configuracion.getIdioma(), configuracion.getNivelZoom(),
                configuracion.getFiltroColor(), configuracion.isFiltroColorActivo());
    }
}
