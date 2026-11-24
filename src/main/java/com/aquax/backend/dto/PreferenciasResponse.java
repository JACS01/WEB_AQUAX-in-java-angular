package com.aquax.backend.dto;

public record PreferenciasResponse(
        Long usuarioId,
        String idioma,
        int nivelZoom,
        String filtroColor,
        boolean filtroColorActivo) {
}
