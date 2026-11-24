package com.aquax.backend.controller;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aquax.backend.dto.ActualizarPreferenciasRequest;
import com.aquax.backend.dto.PreferenciasResponse;
import com.aquax.backend.service.ConfiguracionUsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/personal/preferencias")
public class PreferenciasController {

    private final ConfiguracionUsuarioService configuracionService;

    public PreferenciasController(ConfiguracionUsuarioService configuracionService) {
        this.configuracionService = configuracionService;
    }

    @GetMapping
    public PreferenciasResponse consultar(Principal usuario) {
        return configuracionService.consultar(usuario.getName());
    }

    @PutMapping
    public PreferenciasResponse actualizar(
            @Valid @RequestBody ActualizarPreferenciasRequest solicitud, Principal usuario) {
        return configuracionService.actualizar(usuario.getName(), solicitud);
    }

}
