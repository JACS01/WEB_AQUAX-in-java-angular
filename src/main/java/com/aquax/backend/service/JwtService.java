package com.aquax.backend.service;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.aquax.backend.model.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;

@Service
public class JwtService {

    private static final String EMISOR = "aquax-backend";

    private final SecretKey clave;
    private final long expiracionSegundos;

    public JwtService(
            @Value("${aquax.jwt.secret-base64:}") String secreto,
            @Value("${aquax.jwt.expiration-seconds:3600}") long expiracionSegundos) {
        if (expiracionSegundos <= 0) {
            throw new IllegalArgumentException("La duración del JWT debe ser mayor que cero.");
        }
        this.expiracionSegundos = expiracionSegundos;
        this.clave = construirClave(secreto);
    }

    public long getExpiracionSegundos() {
        return expiracionSegundos;
    }

    public void comprobarConfiguracion() {
        if (clave == null) {
            throw new JwtNoConfiguradoException();
        }
    }

    public String generarToken(Usuario usuario) {
        comprobarConfiguracion();
        Instant ahora = Instant.now();
        return Jwts.builder()
                .issuer(EMISOR)
                .subject(usuario.getNombreUsuario())
                .claim("uid", usuario.getId())
                .claim("ver", usuario.getVersionToken())
                .claim("rol", usuario.getRol().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(expiracionSegundos)))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    public Claims verificarToken(String token) {
        comprobarConfiguracion();
        var firmado = Jwts.parser()
                .verifyWith(clave)
                .requireIssuer(EMISOR)
                .build()
                .parseSignedClaims(token);
        if (!Jwts.SIG.HS256.getId().equals(firmado.getHeader().getAlgorithm())) {
            throw new JwtException("El algoritmo del token no está permitido.");
        }
        Claims claims = firmado.getPayload();
        if (claims.getSubject() == null || claims.getSubject().isBlank()
                || claims.getExpiration() == null
                || claims.get("uid", Long.class) == null
                || claims.get("ver", Long.class) == null) {
            throw new JwtException("El token no contiene la identidad requerida.");
        }
        return claims;
    }

    private SecretKey construirClave(String secreto) {
        if (secreto.isBlank()) {
            return null;
        }
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secreto.trim());
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("AQUAX_JWT_SECRET debe ser una clave Base64 válida.");
        }
        if (bytes.length < 32) {
            throw new IllegalArgumentException("AQUAX_JWT_SECRET debe contener al menos 32 bytes en Base64.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
