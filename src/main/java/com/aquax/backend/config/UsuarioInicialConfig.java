package com.aquax.backend.config;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.aquax.backend.model.RolUsuario;
import com.aquax.backend.model.Usuario;
import com.aquax.backend.repository.UsuarioRepository;

@Configuration(proxyBeanMethods = false)
public class UsuarioInicialConfig {

    @Bean
    ApplicationRunner crearAdministradorInicial(
            UsuarioRepository usuarioRepository,
            PasswordEncoder encoder,
            PlatformTransactionManager transactionManager,
            @Value("${aquax.personal.usuario:personal}") String nombreUsuario,
            @Value("${aquax.personal.contrasena:}") String contrasena) {
        return args -> {
            if (contrasena.isBlank()) {
                return;
            }
            String nombreNormalizado = nombreUsuario.trim().toLowerCase(Locale.ROOT);
            if (!nombreNormalizado.matches("[a-z0-9._-]{3,50}")) {
                throw new IllegalArgumentException(
                        "AQUAX_PERSONAL_USUARIO debe tener entre 3 y 50 letras, números, puntos, guiones o guiones bajos.");
            }
            if (contrasena.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new IllegalArgumentException("AQUAX_PERSONAL_CONTRASENA debe ocupar como máximo 72 bytes en UTF-8.");
            }

            TransactionTemplate transaccion = new TransactionTemplate(transactionManager);
            transaccion.executeWithoutResult(status -> {
                if (usuarioRepository.findByNombreUsuario(nombreNormalizado).isEmpty()) {
                    usuarioRepository.save(new Usuario(
                            nombreNormalizado, "Administrador", "AquaX", nombreNormalizado + "@aquax.local",
                            encoder.encode(contrasena), RolUsuario.ADMINISTRADOR));
                }
            });
        };
    }
}
