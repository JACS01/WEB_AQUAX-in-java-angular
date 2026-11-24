package com.aquax.backend.service;

public class PedidoNoEncontradoException extends RuntimeException {

    public PedidoNoEncontradoException(Long id) {
        super("No existe un pedido con el identificador " + id + ".");
    }
}
