package com.aquax.backend.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aquax.backend.dto.LoginRequest;
import com.aquax.backend.dto.LoginResponse;
import com.aquax.backend.dto.UsuarioResponse;
import com.aquax.backend.model.Usuario;
import com.aquax.backend.repository.UsuarioRepository;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String comprobacionUsuarioInexistente;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.comprobacionUsuarioInexistente = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public LoginResponse iniciarSesion(LoginRequest solicitud) {
        jwtService.comprobarConfiguracion();
        if (solicitud.contrasena().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw credencialesIncorrectas();
        }
        String nombreUsuario = solicitud.nombreUsuario().trim().toLowerCase(Locale.ROOT);
        Optional<Usuario> cuenta = usuarioRepository.buscarPorNombreParaActualizar(nombreUsuario);
        if (cuenta.isEmpty()) {
            passwordEncoder.matches(solicitud.contrasena(), comprobacionUsuarioInexistente);
            throw credencialesIncorrectas();
        }
        Usuario usuario = cuenta.get();
        boolean contrasenaCorrecta = passwordEncoder.matches(solicitud.contrasena(), usuario.getContrasena());
        if (!contrasenaCorrecta || !usuario.isActivo()) {
            throw credencialesIncorrectas();
        }
        UsuarioResponse respuesta = new UsuarioResponse(usuario.getId(), usuario.getNombreUsuario(),
                usuario.getNombre(), usuario.getApellido(), usuario.getCorreo(), usuario.getRol(), usuario.isActivo());
        return new LoginResponse(jwtService.generarToken(usuario), "Bearer", jwtService.getExpiracionSegundos(), respuesta);
    }

    private BadCredentialsException credencialesIncorrectas() {
        return new BadCredentialsException("Usuario o contraseña incorrectos.");
    }
}
