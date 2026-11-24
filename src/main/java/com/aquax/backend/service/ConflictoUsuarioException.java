package com.aquax.backend.service;

public class ConflictoUsuarioException extends RuntimeException {

    public ConflictoUsuarioException(String mensaje) {
        super(mensaje);
    }
}
