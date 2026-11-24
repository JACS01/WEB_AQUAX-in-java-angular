package com.aquax.backend.dto;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        UsuarioResponse usuario) {
}
