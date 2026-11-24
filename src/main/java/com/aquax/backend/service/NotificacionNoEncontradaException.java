package com.aquax.backend.service;

public class NotificacionNoEncontradaException extends RuntimeException {

    public NotificacionNoEncontradaException() {
        super("No se encontró la notificación.");
    }
}
