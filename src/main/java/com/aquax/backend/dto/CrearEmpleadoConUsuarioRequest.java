package com.aquax.backend.dto;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearEmpleadoConUsuarioRequest(
        @NotNull(message = "Los datos de la cuenta son obligatorios.")
        @Valid
        CrearUsuarioRequest usuario,

        @NotBlank(message = "El teléfono es obligatorio.")
        @Size(max = 15, message = "El teléfono no puede exceder 15 caracteres.")
        String telefono,

        @NotBlank(message = "El DNI es obligatorio.")
        @Pattern(regexp = "[0-9]{8}", message = "El DNI debe contener exactamente 8 dígitos.")
        String dni,

        LocalDate fechaIngreso) {
}
