package com.aquax.backend.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CrearEmpleadoRequest(
        @NotNull(message = "El usuario es obligatorio.")
        @Positive(message = "El identificador del usuario debe ser positivo.")
        Long usuarioId,

        @NotBlank(message = "El DNI es obligatorio.")
        @Pattern(regexp = "[0-9]{8}", message = "El DNI debe contener exactamente 8 dígitos.")
        String dni,

        LocalDate fechaIngreso) {
}
