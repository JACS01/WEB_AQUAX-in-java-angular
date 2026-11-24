package com.aquax.backend.controller;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aquax.backend.dto.ActualizarPerfilRequest;
import com.aquax.backend.dto.CambiarContrasenaRequest;
import com.aquax.backend.dto.MensajeResponse;
import com.aquax.backend.dto.PerfilResponse;
import com.aquax.backend.service.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/personal/perfil")
public class PerfilController {

    private final UsuarioService usuarioService;

    public PerfilController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public PerfilResponse consultar(Principal usuario) {
        return usuarioService.consultarPerfil(usuario.getName());
    }

    @PutMapping
    public PerfilResponse actualizar(@Valid @RequestBody ActualizarPerfilRequest solicitud, Principal usuario) {
        return usuarioService.actualizarPerfil(usuario.getName(), solicitud);
    }

    @PatchMapping("/contrasena")
    public MensajeResponse cambiarContrasena(
            @Valid @RequestBody CambiarContrasenaRequest solicitud, Principal usuario) {
        usuarioService.cambiarContrasena(usuario.getName(), solicitud);
        return new MensajeResponse("Contraseña actualizada correctamente.");
    }

}
