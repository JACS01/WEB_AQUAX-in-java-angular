package com.aquax.backend.service;

public class JwtNoConfiguradoException extends RuntimeException {

    public JwtNoConfiguradoException() {
        super("Configura AQUAX_JWT_SECRET para habilitar el acceso con JWT.");
    }
}
