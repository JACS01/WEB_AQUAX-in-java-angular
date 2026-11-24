package com.aquax.backend.dto;

import com.aquax.backend.model.RolUsuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearUsuarioRequest(
        @NotBlank(message = "El nombre de usuario es obligatorio.")
        @Pattern(regexp = "[a-zA-Z0-9._-]{3,50}",
                message = "El usuario debe tener entre 3 y 50 caracteres alfanuméricos, puntos, guiones o guiones bajos.")
        String nombreUsuario,

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

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
        String contrasena,

        @NotNull(message = "El rol es obligatorio.")
        RolUsuario rol) {
}
