package com.aquax.backend.service;

public class ConflictoEmpleadoException extends RuntimeException {

    public ConflictoEmpleadoException(String mensaje) {
        super(mensaje);
    }
}
