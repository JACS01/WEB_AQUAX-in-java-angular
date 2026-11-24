package com.aquax.backend.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Paginacion {

    private Paginacion() {
    }

    public static void validarParametros(int pagina, int tamano) {
        if (pagina < 0 || tamano <= 0) {
            throw new IllegalArgumentException("La página debe ser mayor o igual a cero y el tamaño mayor a cero.");
        }
    }

    public static Pageable segura(Pageable pagina, Map<String, String> campos) {
        List<Sort.Order> ordenes = new ArrayList<>();
        Set<String> usados = new HashSet<>();
        for (Sort.Order orden : pagina.getSort()) {
            String campo = campos.get(orden.getProperty());
            if (campo == null) {
                throw new IllegalArgumentException("El campo de orden no está permitido: " + orden.getProperty());
            }
            if (usados.add(campo)) {
                ordenes.add(orden.withProperty(campo));
            }
        }
        if (usados.add("id")) {
            ordenes.add(Sort.Order.asc("id"));
        }
        return PageRequest.of(pagina.getPageNumber(), Math.min(pagina.getPageSize(), 50), Sort.by(ordenes));
    }

    public static String patron(String texto) {
        String normalizado = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        return "%" + normalizado.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
