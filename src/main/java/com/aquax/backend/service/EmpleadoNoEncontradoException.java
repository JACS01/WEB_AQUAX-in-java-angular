package com.aquax.backend.service;

public class EmpleadoNoEncontradoException extends RuntimeException {

    public EmpleadoNoEncontradoException() {
        super("No se encontró el empleado.");
    }
}
