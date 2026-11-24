package com.aquax.backend.dto;

import com.aquax.backend.model.RolUsuario;

public record PerfilResponse(
        Long id,
        String nombreUsuario,
        String nombre,
        String apellido,
        String correo,
        String telefono,
        RolUsuario rol,
        boolean activo) {
}
