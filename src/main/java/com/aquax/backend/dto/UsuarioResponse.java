package com.aquax.backend.dto;

import com.aquax.backend.model.RolUsuario;

public record UsuarioResponse(
        Long id,
        String nombreUsuario,
        String nombre,
        String apellido,
        String correo,
        RolUsuario rol,
        boolean activo) {
}
