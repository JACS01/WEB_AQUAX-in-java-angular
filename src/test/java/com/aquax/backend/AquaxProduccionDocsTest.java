package com.aquax.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"aquax.personal.usuario=personal", "aquax.personal.contrasena=1234", "aquax.jwt.expiration-seconds=3600", "aquax.jwt.secret-base64="})
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class AquaxProduccionDocsTest {

    private static final Path DB = Path.of("target", "prod-" + UUID.randomUUID() + ".db").toAbsolutePath();
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DB);
    }

    @Test
    void produccionUsaSqliteYDocumentacionDesactivada() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select sqlite_version()", String.class)).isNotBlank();
        assertThat(jdbc.queryForObject("select count(*) from productos", Long.class)).isZero();
    }
}


