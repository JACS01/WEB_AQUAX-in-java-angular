package com.aquax.backend.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aquax.backend.dto.ActualizarPerfilRequest;
import com.aquax.backend.dto.CambiarContrasenaRequest;
import com.aquax.backend.dto.CrearUsuarioRequest;
import com.aquax.backend.dto.PerfilResponse;
import com.aquax.backend.dto.PaginaResponse;
import com.aquax.backend.dto.UsuarioResponse;
import com.aquax.backend.model.RolUsuario;
import com.aquax.backend.model.Usuario;
import com.aquax.backend.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private static final Map<String, String> CAMPOS_ORDEN = Map.of(
            "id", "id", "nombreUsuario", "nombreUsuario", "nombre", "nombre", "apellido", "apellido",
            "correo", "correo", "rol", "rol", "activo", "activo");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse crear(CrearUsuarioRequest solicitud) {
        String nombreUsuario = solicitud.nombreUsuario().toLowerCase(Locale.ROOT);
        String correo = solicitud.correo().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByNombreUsuario(nombreUsuario)
                || usuarioRepository.existsByCorreo(correo)) {
            throw new ConflictoUsuarioException("El nombre de usuario o correo ya está registrado.");
        }
        if (solicitud.contrasena().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La contraseña debe ocupar como máximo 72 bytes en UTF-8.");
        }
        Usuario usuario = new Usuario(nombreUsuario, solicitud.nombre().trim(),
                solicitud.apellido().trim(), correo,
                passwordEncoder.encode(solicitud.contrasena()), solicitud.rol());
        return aRespuesta(usuarioRepository.saveAndFlush(usuario));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll(Sort.by("id")).stream().map(this::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioResponse> listarPagina(String texto, Pageable pagina) {
        Pageable segura = Paginacion.segura(pagina, CAMPOS_ORDEN);
        return PaginaResponse.de(usuarioRepository.buscarPagina(Paginacion.patron(texto), segura)
                .map(this::aRespuesta));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse consultarActual(String nombreUsuario) {
        return aRespuesta(usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new));
    }

    @Transactional(readOnly = true)
    public PerfilResponse consultarPerfil(String nombreUsuario) {
        return aPerfil(usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new));
    }

    @Transactional
    public PerfilResponse actualizarPerfil(String nombreUsuario, ActualizarPerfilRequest solicitud) {
        Usuario usuario = buscarPorNombreParaActualizar(nombreUsuario);
        String correo = solicitud.correo().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByCorreoAndIdNot(correo, usuario.getId())) {
            throw new ConflictoUsuarioException("El correo ya está registrado por otra cuenta.");
        }
        usuario.actualizarPerfil(solicitud.nombre(), solicitud.apellido(), correo, solicitud.telefono());
        return aPerfil(usuarioRepository.saveAndFlush(usuario));
    }

    @Transactional
    public void cambiarContrasena(String nombreUsuario, CambiarContrasenaRequest solicitud) {
        Usuario usuario = buscarPorNombreParaActualizar(nombreUsuario);
        if (!solicitud.nuevaContrasena().equals(solicitud.confirmarContrasena())) {
            throw new IllegalArgumentException("La nueva contraseña y su confirmación no coinciden.");
        }
        if (solicitud.nuevaContrasena().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La nueva contraseña debe ocupar como máximo 72 bytes en UTF-8.");
        }
        if (solicitud.contrasenaActual().getBytes(StandardCharsets.UTF_8).length > 72
                || !passwordEncoder.matches(solicitud.contrasenaActual(), usuario.getContrasena())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta.");
        }
        usuario.cambiarContrasena(passwordEncoder.encode(solicitud.nuevaContrasena()));
    }

    @Transactional
    public UsuarioResponse cambiarActivo(Long id, boolean activo) {
        // Se serializan las bajas de administradores para conservar al menos una cuenta habilitada.
        List<Usuario> administradores = usuarioRepository
                .bloquearAdministradoresActivos(RolUsuario.ADMINISTRADOR);
        Usuario usuario = usuarioRepository.buscarParaActualizar(id)
                .orElseThrow(UsuarioNoEncontradoException::new);
        if (!activo && usuario.isActivo() && usuario.getRol() == RolUsuario.ADMINISTRADOR
                && administradores.size() == 1) {
            throw new ConflictoUsuarioException("Debe permanecer al menos un administrador activo.");
        }
        usuario.cambiarActivo(activo);
        return aRespuesta(usuario);
    }

    @Transactional
    public UsuarioResponse cambiarRol(Long id, RolUsuario nuevoRol) {
        List<Usuario> administradores = usuarioRepository
                .bloquearAdministradoresActivos(RolUsuario.ADMINISTRADOR);
        Usuario usuario = usuarioRepository.buscarParaActualizar(id)
                .orElseThrow(UsuarioNoEncontradoException::new);
        if (usuario.getRol() != nuevoRol && usuario.isActivo()
                && usuario.getRol() == RolUsuario.ADMINISTRADOR && administradores.size() == 1) {
            throw new ConflictoUsuarioException("Debe permanecer al menos un administrador activo.");
        }
        usuario.cambiarRol(nuevoRol);
        return aRespuesta(usuario);
    }

    private UsuarioResponse aRespuesta(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombreUsuario(), usuario.getNombre(),
                usuario.getApellido(), usuario.getCorreo(), usuario.getRol(), usuario.isActivo());
    }

    private Usuario buscarPorNombreParaActualizar(String nombreUsuario) {
        return usuarioRepository.buscarPorNombreParaActualizar(nombreUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);
    }

    private PerfilResponse aPerfil(Usuario usuario) {
        return new PerfilResponse(usuario.getId(), usuario.getNombreUsuario(), usuario.getNombre(),
                usuario.getApellido(), usuario.getCorreo(), usuario.getTelefono(),
                usuario.getRol(), usuario.isActivo());
    }
}
