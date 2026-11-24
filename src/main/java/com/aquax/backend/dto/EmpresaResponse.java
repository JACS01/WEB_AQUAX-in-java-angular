package com.aquax.backend.dto;

import java.time.LocalDate;

public record EmpresaResponse(
        Long id,
        String razonSocial,
        String ruc,
        String direccion,
        String telefono1,
        String telefono2,
        String correo,
        String registroSanitario,
        String slogan,
        LocalDate ultimaActualizacion) {
}
