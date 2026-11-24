package com.aquax.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ActualizarPreferenciasRequest(
        @NotBlank(message = "El idioma es obligatorio.")
        @Pattern(regexp = "es|en|pt", message = "El idioma debe ser es, en o pt.")
        String idioma,

        @NotNull(message = "El nivel de zoom es obligatorio.")
        Integer nivelZoom,

        @NotBlank(message = "El filtro de color es obligatorio.")
        @Pattern(regexp = "none|deuteranopia|protanopia|tritanopia|grayscale|contrast|warm|cool|sepia|invert",
                message = "El filtro de color no es válido.")
        String filtroColor,

        @NotNull(message = "El estado del filtro de color es obligatorio.")
        Boolean filtroColorActivo) {
}
