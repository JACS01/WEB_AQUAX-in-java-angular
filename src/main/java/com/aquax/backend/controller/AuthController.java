package com.aquax.backend.controller;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aquax.backend.dto.LoginRequest;
import com.aquax.backend.dto.LoginResponse;
import com.aquax.backend.dto.UsuarioResponse;
import com.aquax.backend.service.AuthService;
import com.aquax.backend.service.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;
    private final AuthService authService;

    public AuthController(UsuarioService usuarioService, AuthService authService) {
        this.usuarioService = usuarioService;
        this.authService = authService;
    }

    @GetMapping("/me")
    public UsuarioResponse consultarActual(Principal usuario) {
        return usuarioService.consultarActual(usuario.getName());
    }

    @PostMapping("/login")
    public LoginResponse iniciarSesion(@Valid @RequestBody LoginRequest solicitud) {
        return authService.iniciarSesion(solicitud);
    }

}
