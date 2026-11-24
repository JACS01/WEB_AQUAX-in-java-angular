package com.aquax.backend.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarActivoUsuarioRequest(
        @NotNull(message = "El estado activo es obligatorio.")
        Boolean activo) {
}
