package com.aquax.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"aquax.jwt.secret-base64=", "aquax.personal.usuario=personal", "aquax.personal.contrasena=1234", "aquax.jwt.expiration-seconds=3600"})
@org.springframework.test.context.ActiveProfiles("sqlite")
@AutoConfigureMockMvc
class AquaxJwtSinClaveTest {

    private static final Path DB = Path.of("target", "sin-jwt-" + UUID.randomUUID() + ".db").toAbsolutePath();
    @Autowired MockMvc mvc;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DB);
    }

    @Test
    void basicFuncionaYLoginExplicaLaConfiguracionPendiente() throws Exception {
        String basic = "Basic " + Base64.getEncoder().encodeToString("personal:1234".getBytes(StandardCharsets.UTF_8));
        mvc.perform(get("/api/auth/me").header("Authorization", basic)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"nombreUsuario":"personal","contrasena":"1234"}
                """)).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").exists());
    }
}


