package com.aquax.backend.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class ErroresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ErroresSeguridad.class);
    private static final String BASIC = "Basic realm=\"AquaX\"";

    private final ProblemasApi problemas;

    public ErroresSeguridad(ProblemasApi problemas) {
        this.problemas = problemas;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException error)
            throws IOException {
        if (error instanceof AuthenticationServiceException) {
            LOG.error("No se pudo verificar la autenticación. Tipo de fallo: {}", error.getClass().getName());
            problemas.escribir(request, response, HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo completar la solicitud. Inténtalo nuevamente.");
            return;
        }
        String autorizacion = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (autorizacion != null && autorizacion.regionMatches(true, 0, "Bearer ", 0, 7)) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        } else if (autorizacion != null && autorizacion.regionMatches(true, 0, "Basic ", 0, 6)) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, BASIC);
        } else {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            response.addHeader(HttpHeaders.WWW_AUTHENTICATE, BASIC);
        }
        problemas.escribir(request, response, HttpStatus.UNAUTHORIZED,
                "Se requieren credenciales válidas para acceder a este recurso.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException error)
            throws IOException {
        problemas.escribir(request, response, HttpStatus.FORBIDDEN,
                "No tienes permisos para acceder a este recurso.");
    }
}
