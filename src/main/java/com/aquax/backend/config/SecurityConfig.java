package com.aquax.backend.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.aquax.backend.repository.UsuarioRepository;
import com.aquax.backend.service.JwtService;

import jakarta.servlet.DispatcherType;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtService jwtService, UsuarioRepository usuarioRepository, ProblemasApi problemas) {
        PathPatternRequestMatcher.Builder rutas = PathPatternRequestMatcher.withDefaults();
        ErroresSeguridad errores = new ErroresSeguridad(problemas);

        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        rutas.matcher(HttpMethod.POST, "/api/auth/login"),
                        rutas.matcher(HttpMethod.POST, "/api/pedidos"),
                        rutas.matcher(HttpMethod.POST, "/api/personal/pedidos"),
                        rutas.matcher(HttpMethod.POST, "/api/admin/usuarios"),
                        rutas.matcher(HttpMethod.POST, "/api/admin/empleados"),
                        rutas.matcher(HttpMethod.POST, "/api/admin/empleados/con-usuario"),
                        rutas.matcher(HttpMethod.PUT, "/api/admin/empleados/{id}"),
                        rutas.matcher(HttpMethod.DELETE, "/api/admin/empleados/{id}"),
                        rutas.matcher(HttpMethod.PUT, "/api/admin/empleados/{id}/perfil"),
                        rutas.matcher(HttpMethod.PATCH, "/api/admin/empleados/{id}/rol"),
                        rutas.matcher(HttpMethod.PATCH, "/api/admin/empleados/{id}/activo"),
                        rutas.matcher(HttpMethod.PUT, "/api/admin/empresa"),
                        rutas.matcher(HttpMethod.PUT, "/api/personal/perfil"),
                        rutas.matcher(HttpMethod.PUT, "/api/personal/preferencias"),
                        rutas.matcher(HttpMethod.PATCH, "/api/personal/perfil/contrasena"),
                        rutas.matcher(HttpMethod.PATCH, "/api/admin/usuarios/{id}/activo"),
                        rutas.matcher(HttpMethod.PATCH, "/api/personal/notificaciones/{id}/leida"),
                        rutas.matcher(HttpMethod.PATCH, "/api/personal/notificaciones/leidas"),
                        rutas.matcher(HttpMethod.PATCH, "/api/personal/pedidos/{id}/entrega"),
                        rutas.matcher(HttpMethod.PATCH, "/api/personal/pedidos/{id}/pago")))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/productos").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/pedidos").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/personal/**").hasAnyRole("ADMINISTRADOR", "VENDEDOR")
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").hasAnyRole("ADMINISTRADOR", "VENDEDOR")
                        .anyRequest().denyAll())
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint(errores)
                        .accessDeniedHandler(errores))
                .httpBasic(basic -> basic.authenticationEntryPoint(errores))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, usuarioRepository, problemas),
                        BasicAuthenticationFilter.class)
                .build();
    }

    @Bean
    UrlBasedCorsConfigurationSource corsConfigurationSource(
            @Value("${aquax.cors.allowed-origins:http://localhost:4200}") List<String> origenes) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(origenes);
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT));
        configuracion.setExposedHeaders(List.of(HttpHeaders.CONTENT_DISPOSITION));
        configuracion.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
