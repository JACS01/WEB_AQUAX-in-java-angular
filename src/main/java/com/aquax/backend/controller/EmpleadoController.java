package com.aquax.backend.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aquax.backend.dto.ActualizarEmpleadoRequest;
import com.aquax.backend.dto.ActualizarPerfilRequest;
import com.aquax.backend.dto.CambiarActivoUsuarioRequest;
import com.aquax.backend.dto.CambiarRolUsuarioRequest;
import com.aquax.backend.dto.CrearEmpleadoConUsuarioRequest;
import com.aquax.backend.dto.CrearEmpleadoRequest;
import com.aquax.backend.dto.EmpleadoResponse;
import com.aquax.backend.dto.PaginaResponse;
import com.aquax.backend.service.EmpleadoService;
import com.aquax.backend.service.Paginacion;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/empleados")
public class EmpleadoController {

    private final EmpleadoService empleadoService;

    public EmpleadoController(EmpleadoService empleadoService) {
        this.empleadoService = empleadoService;
    }

    @GetMapping
    public List<EmpleadoResponse> listar() {
        return empleadoService.listar();
    }

    @GetMapping("/pagina")
    public PaginaResponse<EmpleadoResponse> listarPagina(
            @RequestParam(required = false) String texto,
            @PageableDefault(size = 10, sort = "nombre") Pageable pageable,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Paginacion.validarParametros(page, size);
        return empleadoService.listarPagina(texto, pageable);
    }

    @GetMapping("/{id}")
    public EmpleadoResponse consultar(@PathVariable Long id) {
        return empleadoService.consultar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        empleadoService.eliminar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmpleadoResponse registrar(@Valid @RequestBody CrearEmpleadoRequest solicitud) {
        return empleadoService.registrar(solicitud);
    }

    @PostMapping("/con-usuario")
    @ResponseStatus(HttpStatus.CREATED)
    public EmpleadoResponse registrarConUsuario(@Valid @RequestBody CrearEmpleadoConUsuarioRequest solicitud) {
        return empleadoService.registrarConUsuario(solicitud);
    }

    @PutMapping("/{id}")
    public EmpleadoResponse actualizar(
            @PathVariable Long id, @Valid @RequestBody ActualizarEmpleadoRequest solicitud) {
        return empleadoService.actualizar(id, solicitud);
    }

    @PatchMapping("/{id}/activo")
    public EmpleadoResponse cambiarActivo(
            @PathVariable Long id, @Valid @RequestBody CambiarActivoUsuarioRequest solicitud) {
        return empleadoService.cambiarActivo(id, solicitud.activo());
    }

    @PutMapping("/{id}/perfil")
    public EmpleadoResponse actualizarPerfil(
            @PathVariable Long id, @Valid @RequestBody ActualizarPerfilRequest solicitud) {
        return empleadoService.actualizarPerfil(id, solicitud);
    }

    @PatchMapping("/{id}/rol")
    public EmpleadoResponse cambiarRol(
            @PathVariable Long id, @Valid @RequestBody CambiarRolUsuarioRequest solicitud) {
        return empleadoService.cambiarRol(id, solicitud.rol());
    }

}
