package com.aquax.backend.dto;

import com.aquax.backend.model.RolUsuario;

import jakarta.validation.constraints.NotNull;

public record CambiarRolUsuarioRequest(
        @NotNull(message = "El rol es obligatorio.")
        RolUsuario rol) {
}
