package com.aquax.backend.service;

import java.util.Locale;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aquax.backend.model.Usuario;
import com.aquax.backend.repository.UsuarioRepository;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String nombreUsuario) {
        String nombreNormalizado = nombreUsuario.trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByNombreUsuario(nombreNormalizado)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado."));
        return User.withUsername(usuario.getNombreUsuario())
                .password(usuario.getContrasena())
                .roles(usuario.getRol().name())
                .disabled(!usuario.isActivo())
                .build();
    }
}
