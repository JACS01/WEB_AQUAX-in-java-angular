package com.aquax.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El nombre de usuario es obligatorio.")
        @Size(max = 50, message = "El nombre de usuario no puede exceder 50 caracteres.")
        String nombreUsuario,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(max = 72, message = "La contraseña no puede exceder 72 caracteres.")
        String contrasena) {
}
