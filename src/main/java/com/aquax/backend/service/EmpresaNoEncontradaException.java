package com.aquax.backend.service;

public class EmpresaNoEncontradaException extends RuntimeException {

    public EmpresaNoEncontradaException() {
        super("No se encontraron los ajustes empresariales.");
    }
}
