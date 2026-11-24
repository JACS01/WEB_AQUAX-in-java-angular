package com.aquax.backend.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aquax.backend.dto.CambiarActivoUsuarioRequest;
import com.aquax.backend.dto.CrearUsuarioRequest;
import com.aquax.backend.dto.UsuarioResponse;
import com.aquax.backend.dto.PaginaResponse;
import com.aquax.backend.service.UsuarioService;
import com.aquax.backend.service.Paginacion;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/pagina")
    public PaginaResponse<UsuarioResponse> listarPagina(
            @RequestParam(required = false) String texto,
            @PageableDefault(size = 10, sort = "nombre") Pageable pageable,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Paginacion.validarParametros(page, size);
        return usuarioService.listarPagina(texto, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody CrearUsuarioRequest solicitud) {
        return usuarioService.crear(solicitud);
    }

    @PatchMapping("/{id}/activo")
    public UsuarioResponse cambiarActivo(@PathVariable Long id,
                                          @Valid @RequestBody CambiarActivoUsuarioRequest solicitud) {
        return usuarioService.cambiarActivo(id, solicitud.activo());
    }

}
