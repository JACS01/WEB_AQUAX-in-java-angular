package com.aquax.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarContrasenaRequest(
        @NotBlank(message = "La contraseña actual es obligatoria.")
        @Size(max = 72, message = "La contraseña actual no puede exceder 72 caracteres.")
        String contrasenaActual,

        @NotBlank(message = "La nueva contraseña es obligatoria.")
        @Size(min = 8, max = 72, message = "La nueva contraseña debe tener entre 8 y 72 caracteres.")
        String nuevaContrasena,

        @NotBlank(message = "La confirmación de contraseña es obligatoria.")
        @Size(max = 72, message = "La confirmación no puede exceder 72 caracteres.")
        String confirmarContrasena) {
}
