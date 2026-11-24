package com.aquax.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarEmpresaRequest(
        @NotBlank(message = "La razón social es obligatoria.")
        @Size(max = 150, message = "La razón social no puede exceder 150 caracteres.")
        String razonSocial,

        @NotBlank(message = "El RUC es obligatorio.")
        @Pattern(regexp = "[0-9]{11}", message = "El RUC debe contener exactamente 11 dígitos.")
        String ruc,

        @NotBlank(message = "La dirección es obligatoria.")
        @Size(max = 300, message = "La dirección no puede exceder 300 caracteres.")
        String direccion,

        @Size(max = 15, message = "El teléfono principal no puede exceder 15 caracteres.")
        String telefono1,

        @Size(max = 15, message = "El teléfono secundario no puede exceder 15 caracteres.")
        String telefono2,

        @Email(message = "El correo debe tener un formato válido.")
        @Size(max = 200, message = "El correo no puede exceder 200 caracteres.")
        String correo,

        @Size(max = 500, message = "El registro sanitario no puede exceder 500 caracteres.")
        String registroSanitario,

        @Size(max = 200, message = "El eslogan no puede exceder 200 caracteres.")
        String slogan) {
}
