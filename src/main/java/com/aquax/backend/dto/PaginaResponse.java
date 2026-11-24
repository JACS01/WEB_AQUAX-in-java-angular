package com.aquax.backend.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record PaginaResponse<T>(List<T> content, PageMetadata page) {

    public static <T> PaginaResponse<T> de(Page<T> pagina) {
        return new PaginaResponse<>(pagina.getContent(), new PageMetadata(
                pagina.getSize(), pagina.getNumber(), pagina.getTotalElements(), pagina.getTotalPages()));
    }

    public record PageMetadata(int size, int number, long totalElements, int totalPages) {
    }
}
