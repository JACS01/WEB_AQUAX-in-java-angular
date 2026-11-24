package com.aquax.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.aquax.backend.repository.ConfiguracionUsuarioRepository;

@SpringBootTest(properties = {"aquax.personal.usuario=personal", "aquax.personal.contrasena=1234", "aquax.jwt.secret-base64=", "aquax.jwt.expiration-seconds=3600", "aquax.cors.allowed-origins=http://localhost:4200"})
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("sqlite")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class AquaxPreferenciasTest {
    private static final java.nio.file.Path AUDITORIA_DB = java.nio.file.Path.of(
            "target", "auditoria-" + java.util.UUID.randomUUID() + ".db").toAbsolutePath();

    @org.springframework.test.context.DynamicPropertySource
    static void configurarAuditoria(org.springframework.test.context.DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> "jdbc:sqlite:" + AUDITORIA_DB);
    }

    private static final String RUTA = "/api/personal/preferencias";
    private static final String ADMIN = basic("personal:1234");
    private static final String VENDEDOR = basic("vendedor1:clave-vendedor");
    private static final String CONCURRENTE = basic("vendedor2:clave-vendedor");
    private static final String AJUSTES_ADMIN = """
            {"usuarioId":2,"id":2,"idioma":"en","nivelZoom":80,
             "filtroColor":"contrast","filtroColorActivo":true}
            """;
    private static final String AJUSTES_VENDEDOR = """
            {"usuarioId":1,"idioma":"pt","nivelZoom":150,
             "filtroColor":"grayscale","filtroColorActivo":false}
            """;

    @Autowired MockMvc mvc;
    @Autowired ConfiguracionUsuarioRepository configuracionRepository;

    @Test
    void cadaCuentaGuardaPreferenciasValidasSinCruzarDatosNiCrearDuplicados() throws Exception {
        crearVendedor("vendedor1", "ana@example.com");
        crearVendedor("vendedor2", "otra@example.com");
        assertThat(configuracionRepository.count()).isZero();

        mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        mvc.perform(put(RUTA).contentType(MediaType.APPLICATION_JSON).content(AJUSTES_ADMIN))
                .andExpect(status().isUnauthorized());
        consultar(basic("personal:incorrecta")).andExpect(status().isUnauthorized());

        consultar(ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(1))
                .andExpect(jsonPath("$.idioma").value("es"))
                .andExpect(jsonPath("$.nivelZoom").value(100))
                .andExpect(jsonPath("$.filtroColor").value("none"))
                .andExpect(jsonPath("$.filtroColorActivo").value(false))
                .andExpect(jsonPath("$.contrasena").doesNotExist());
        consultar(VENDEDOR).andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(2))
                .andExpect(jsonPath("$.idioma").value("es"));
        assertThat(configuracionRepository.count()).isZero();

        actualizar(ADMIN, AJUSTES_ADMIN).andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(1))
                .andExpect(jsonPath("$.idioma").value("en"))
                .andExpect(jsonPath("$.nivelZoom").value(80))
                .andExpect(jsonPath("$.filtroColor").value("contrast"))
                .andExpect(jsonPath("$.filtroColorActivo").value(true));
        consultar(VENDEDOR).andExpect(status().isOk())
                .andExpect(jsonPath("$.idioma").value("es"))
                .andExpect(jsonPath("$.nivelZoom").value(100));

        actualizar(VENDEDOR, AJUSTES_VENDEDOR).andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(2))
                .andExpect(jsonPath("$.idioma").value("pt"))
                .andExpect(jsonPath("$.nivelZoom").value(150))
                .andExpect(jsonPath("$.filtroColor").value("grayscale"))
                .andExpect(jsonPath("$.filtroColorActivo").value(false));
        consultar(ADMIN).andExpect(status().isOk()).andExpect(jsonPath("$.idioma").value("en"));
        assertThat(configuracionRepository.count()).isEqualTo(2);

        String[] invalidos = {
                AJUSTES_VENDEDOR.replace("\"pt\"", "\"fr\""),
                AJUSTES_VENDEDOR.replace("\"pt\"", "null"),
                AJUSTES_VENDEDOR.replace("\"pt\"", "\"\""),
                AJUSTES_VENDEDOR.replace("150", "125"),
                AJUSTES_VENDEDOR.replace("150", "140"),
                AJUSTES_VENDEDOR.replace("150", "null"),
                AJUSTES_VENDEDOR.replace("\"grayscale\"", "\"desconocido\""),
                AJUSTES_VENDEDOR.replace("\"grayscale\"", "null"),
                AJUSTES_VENDEDOR.replace("false", "null")
        };
        for (String invalido : invalidos) {
            actualizar(VENDEDOR, invalido).andExpect(status().isBadRequest());
        }
        consultar(VENDEDOR).andExpect(status().isOk())
                .andExpect(jsonPath("$.idioma").value("pt"))
                .andExpect(jsonPath("$.nivelZoom").value(150))
                .andExpect(jsonPath("$.filtroColor").value("grayscale"))
                .andExpect(jsonPath("$.filtroColorActivo").value(false));
        actualizar(VENDEDOR, AJUSTES_VENDEDOR).andExpect(status().isOk());
        assertThat(configuracionRepository.count()).isEqualTo(2);

        CountDownLatch inicio = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Integer> primera = executor.submit(() -> {
                inicio.await();
                return actualizar(CONCURRENTE, AJUSTES_ADMIN).andReturn().getResponse().getStatus();
            });
            Future<Integer> segunda = executor.submit(() -> {
                inicio.await();
                return actualizar(CONCURRENTE, AJUSTES_VENDEDOR).andReturn().getResponse().getStatus();
            });
            inicio.countDown();
            assertThat(primera.get(20, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(segunda.get(20, TimeUnit.SECONDS)).isEqualTo(200);
        }
        assertThat(configuracionRepository.count()).isEqualTo(3);
        assertThat(configuracionRepository.findByUsuario_Id(3L)).isPresent();
        consultar(CONCURRENTE).andExpect(status().isOk()).andExpect(jsonPath("$.usuarioId").value(3));

        actualizar(ADMIN, AJUSTES_ADMIN.replace("true", "false")).andExpect(status().isOk())
                .andExpect(jsonPath("$.filtroColorActivo").value(false));
        consultar(ADMIN).andExpect(status().isOk()).andExpect(jsonPath("$.filtroColorActivo").value(false));

        mvc.perform(patch("/api/admin/usuarios/3/activo").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk());
        consultar(CONCURRENTE).andExpect(status().isUnauthorized());
        actualizar(CONCURRENTE, AJUSTES_ADMIN).andExpect(status().isUnauthorized());
    }

    private void crearVendedor(String usuario, String correo) throws Exception {
        mvc.perform(post("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombreUsuario":"%s","nombre":"Ana","apellido":"Perez","correo":"%s",
                                 "contrasena":"clave-vendedor","rol":"VENDEDOR"}
                                """.formatted(usuario, correo)))
                .andExpect(status().isCreated());
    }

    private ResultActions consultar(String credenciales) throws Exception {
        return mvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, credenciales));
    }

    private ResultActions actualizar(String credenciales, String cuerpo) throws Exception {
        return mvc.perform(put(RUTA).header(HttpHeaders.AUTHORIZATION, credenciales)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private static String basic(String credenciales) {
        return "Basic " + Base64.getEncoder().encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));
    }
}


