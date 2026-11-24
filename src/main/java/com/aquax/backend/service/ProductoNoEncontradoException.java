package com.aquax.backend.service;

public class ProductoNoEncontradoException extends RuntimeException {

    public ProductoNoEncontradoException(Long productoId) {
        super("No existe un producto con el identificador " + productoId + ".");
    }
}
