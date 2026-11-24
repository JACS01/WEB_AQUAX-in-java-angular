package com.aquax.backend.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ActualizarEmpleadoRequest(
        @NotBlank(message = "El DNI es obligatorio.")
        @Pattern(regexp = "[0-9]{8}", message = "El DNI debe contener exactamente 8 dígitos.")
        String dni,

        @NotNull(message = "La fecha de ingreso es obligatoria.")
        LocalDate fechaIngreso) {
}
