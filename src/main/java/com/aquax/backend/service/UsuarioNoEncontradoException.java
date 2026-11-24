package com.aquax.backend.service;

public class UsuarioNoEncontradoException extends RuntimeException {

    public UsuarioNoEncontradoException() {
        super("No se encontró el usuario.");
    }
}
