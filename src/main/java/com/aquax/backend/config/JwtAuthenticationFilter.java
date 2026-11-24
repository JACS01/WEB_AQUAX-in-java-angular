package com.aquax.backend.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.aquax.backend.model.Usuario;
import com.aquax.backend.repository.UsuarioRepository;
import com.aquax.backend.service.JwtNoConfiguradoException;
import com.aquax.backend.service.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final ProblemasApi problemas;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository, ProblemasApi problemas) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.problemas = problemas;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String autorizacion = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (autorizacion == null || !autorizacion.regionMatches(true, 0, "Bearer ", 0, 7)) {
            chain.doFilter(request, response);
            return;
        }
        Claims claims;
        try {
            claims = jwtService.verificarToken(autorizacion.substring(7).trim());
        } catch (JwtException | JwtNoConfiguradoException | IllegalArgumentException error) {
            responderNoAutorizado(request, response);
            return;
        } catch (RuntimeException error) {
            responderErrorInterno(request, response, error);
            return;
        }
        Usuario usuario;
        try {
            usuario = usuarioRepository.findByNombreUsuario(claims.getSubject()).orElse(null);
        } catch (RuntimeException error) {
            responderErrorInterno(request, response, error);
            return;
        }
        try {
            if (usuario == null || !usuario.isActivo() || !usuario.getId().equals(claims.get("uid", Long.class))
                    || usuario.getVersionToken() != claims.get("ver", Long.class)) {
                throw new JwtException("El token ya no es válido para la cuenta.");
            }
            var principal = User.withUsername(usuario.getNombreUsuario())
                    .password("")
                    .roles(usuario.getRol().name())
                    .build();
            var autenticacion = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        } catch (JwtException error) {
            responderNoAutorizado(request, response);
            return;
        } catch (RuntimeException error) {
            responderErrorInterno(request, response, error);
            return;
        }
        chain.doFilter(request, response);
    }

    private void responderNoAutorizado(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        problemas.escribir(request, response, HttpStatus.UNAUTHORIZED,
                "El token es inválido o ha vencido.");
    }

    private void responderErrorInterno(HttpServletRequest request, HttpServletResponse response, RuntimeException error)
            throws IOException {
        SecurityContextHolder.clearContext();
        LOG.error("No se pudo verificar la cuenta al autenticar el JWT. Tipo de fallo: {}",
                error.getClass().getName());
        problemas.escribir(request, response, HttpStatus.INTERNAL_SERVER_ERROR,
                "No se pudo completar la solicitud. Inténtalo nuevamente.");
    }
}
