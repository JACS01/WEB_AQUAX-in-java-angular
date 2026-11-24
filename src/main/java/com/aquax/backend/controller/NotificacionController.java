package com.aquax.backend.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aquax.backend.dto.ContadorNotificacionesResponse;
import com.aquax.backend.dto.NotificacionResponse;
import com.aquax.backend.dto.NotificacionesLeidasResponse;
import com.aquax.backend.service.NotificacionService;

@RestController
@RequestMapping("/api/personal/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public List<NotificacionResponse> listar(
            @RequestParam(defaultValue = "true") boolean soloNoLeidas, Principal usuario) {
        return notificacionService.listar(usuario.getName(), soloNoLeidas);
    }

    @GetMapping("/no-leidas")
    public ContadorNotificacionesResponse contarNoLeidas(Principal usuario) {
        return new ContadorNotificacionesResponse(notificacionService.contarNoLeidas(usuario.getName()));
    }

    @PatchMapping("/{id}/leida")
    public NotificacionResponse marcarComoLeida(@PathVariable Long id, Principal usuario) {
        return notificacionService.marcarComoLeida(id, usuario.getName());
    }

    @PatchMapping("/leidas")
    public NotificacionesLeidasResponse marcarTodasComoLeidas(Principal usuario) {
        return new NotificacionesLeidasResponse(notificacionService.marcarTodasComoLeidas(usuario.getName()));
    }

}
