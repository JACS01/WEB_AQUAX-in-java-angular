package com.aquax.backend.dto;

import java.time.LocalDate;

import com.aquax.backend.model.RolUsuario;

public record EmpleadoResponse(
        Long id,
        String dni,
        LocalDate fechaIngreso,
        Long usuarioId,
        String nombreUsuario,
        String nombre,
        String apellido,
        String correo,
        String telefono,
        RolUsuario rol,
        boolean activo) {
}
