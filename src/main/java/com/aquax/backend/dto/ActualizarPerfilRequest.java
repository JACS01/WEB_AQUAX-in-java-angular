package com.aquax.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarPerfilRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres.")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio.")
        @Size(max = 100, message = "El apellido no puede exceder 100 caracteres.")
        String apellido,

        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo debe tener un formato válido.")
        @Size(max = 150, message = "El correo no puede exceder 150 caracteres.")
        String correo,

        @NotBlank(message = "El teléfono es obligatorio.")
        @Size(max = 15, message = "El teléfono no puede exceder 15 caracteres.")
        String telefono) {
}
