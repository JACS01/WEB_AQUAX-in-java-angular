package com.aquax.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"aquax.personal.usuario=personal", "aquax.personal.contrasena=1234", "aquax.jwt.expiration-seconds=3600"})
@org.springframework.test.context.ActiveProfiles("sqlite")
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AquaxJwtMigracionTest {

    private static final Path DB = crearBaseAnterior();
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> "jdbc:sqlite:" + DB);
        properties.add("aquax.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(
                "0123456789abcdef".repeat(4).getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @Order(1)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void actualizaEsquemaPrevioSinRecrearLaCuenta() throws Exception {
        assertThat(jdbc.queryForObject("select count(*) from usuarios", Long.class)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("select nombre from usuarios where id=1", String.class))
                .isEqualTo("Cuenta conservada");
        assertThat(jdbc.queryForObject("select version_token from usuarios where id=1", Long.class)).isZero();
        String respuesta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"nombreUsuario":"personal","contrasena":"ClavePersistente2026!"}
                """)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(respuesta, "$.token");
        jdbc.update("update ajustes_empresariales set slogan='Conservado con JWT' where id=1");
        jdbc.execute("create table prueba_token (token text not null)");
        jdbc.update("insert into prueba_token(token) values (?)", token);
        assertThat(jdbc.queryForList("pragma foreign_key_check")).isEmpty();
    }

    @Test
    @Order(2)
    void reinicioConservaVersionCuentaYJwtFirmado() throws Exception {
        String token = jdbc.queryForObject("select token from prueba_token", String.class);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Cuenta conservada"));
        assertThat(jdbc.queryForObject("select slogan from ajustes_empresariales where id=1", String.class))
                .isEqualTo("Conservado con JWT");
        assertThat(jdbc.queryForObject("select count(*) from productos", Long.class)).isEqualTo(3L);
    }

    private static Path crearBaseAnterior() {
        try {
            Path archivo = Path.of("target", "migracion-jwt-" + UUID.randomUUID() + ".db").toAbsolutePath();
            Files.createDirectories(archivo.getParent());
            try (var conexion = DriverManager.getConnection("jdbc:sqlite:" + archivo);
                 var sentencia = conexion.createStatement()) {
                sentencia.execute("""
                        create table usuarios (
                          id integer primary key autoincrement,
                          nombre_usuario varchar(50) not null unique,
                          nombre varchar(100) not null,
                          apellido varchar(100) not null,
                          correo varchar(150) not null unique,
                          telefono varchar(15),
                          contrasena varchar(100) not null,
                          rol varchar(20) not null,
                          activo boolean not null)
                        """);
                try (var insertar = conexion.prepareStatement("""
                        insert into usuarios(nombre_usuario,nombre,apellido,correo,contrasena,rol,activo)
                        values ('personal','Cuenta conservada','AquaX','conservada@example.com',?,'ADMINISTRADOR',1)
                        """)) {
                    insertar.setString(1, new BCryptPasswordEncoder().encode("ClavePersistente2026!"));
                    insertar.executeUpdate();
                }
            }
            return archivo;
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }
}


