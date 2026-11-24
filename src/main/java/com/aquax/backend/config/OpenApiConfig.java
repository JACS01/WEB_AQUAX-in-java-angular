package com.aquax.backend.config;

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    private static final Map<String, String> MODULOS = Map.ofEntries(
            entry("/api/auth", "Autenticación"),
            entry("/api/productos", "Catálogo"),
            entry("/api/pedidos", "Pedidos de clientes"),
            entry("/api/admin/usuarios", "Usuarios"),
            entry("/api/admin/empleados", "Empleados"),
            entry("/api/admin/empresa", "Empresa"),
            entry("/api/personal/perfil", "Perfil"),
            entry("/api/personal/preferencias", "Preferencias"),
            entry("/api/personal/notificaciones", "Notificaciones"),
            entry("/api/personal/pedidos", "Pedidos del personal"),
            entry("/api/personal/dashboard", "Dashboard"),
            entry("/api/personal/reportes", "Reportes"));

    private static final Map<String, String> RESUMENES = Map.ofEntries(
            entry("POST /api/auth/login", "Autenticarse y obtener un JWT"),
            entry("GET /api/auth/me", "Consultar la cuenta autenticada"),
            entry("GET /api/productos", "Consultar el catálogo"),
            entry("POST /api/pedidos", "Registrar un pedido de cliente"),
            entry("GET /api/admin/usuarios", "Listar usuarios"),
            entry("GET /api/admin/usuarios/pagina", "Buscar usuarios por páginas"),
            entry("POST /api/admin/usuarios", "Crear una cuenta de usuario"),
            entry("PATCH /api/admin/usuarios/{id}/activo", "Activar o desactivar una cuenta"),
            entry("GET /api/admin/empleados", "Listar empleados"),
            entry("GET /api/admin/empleados/pagina", "Buscar empleados por páginas"),
            entry("GET /api/admin/empleados/{id}", "Consultar una ficha laboral"),
            entry("POST /api/admin/empleados", "Vincular una cuenta existente con una ficha laboral"),
            entry("POST /api/admin/empleados/con-usuario", "Crear un empleado y su cuenta"),
            entry("PUT /api/admin/empleados/{id}", "Actualizar el DNI y la fecha de ingreso"),
            entry("PUT /api/admin/empleados/{id}/perfil", "Actualizar los datos personales del empleado"),
            entry("PATCH /api/admin/empleados/{id}/rol", "Cambiar el rol del empleado"),
            entry("PATCH /api/admin/empleados/{id}/activo", "Activar o desactivar el acceso del empleado"),
            entry("DELETE /api/admin/empleados/{id}", "Eliminar la ficha laboral"),
            entry("GET /api/admin/empresa", "Consultar los datos empresariales"),
            entry("PUT /api/admin/empresa", "Actualizar los datos empresariales"),
            entry("GET /api/personal/perfil", "Consultar el perfil propio"),
            entry("PUT /api/personal/perfil", "Actualizar el perfil propio"),
            entry("PATCH /api/personal/perfil/contrasena", "Cambiar la contraseña propia"),
            entry("GET /api/personal/preferencias", "Consultar las preferencias propias"),
            entry("PUT /api/personal/preferencias", "Guardar las preferencias propias"),
            entry("GET /api/personal/notificaciones", "Consultar las notificaciones propias"),
            entry("GET /api/personal/notificaciones/no-leidas", "Contar las notificaciones sin leer"),
            entry("PATCH /api/personal/notificaciones/{id}/leida", "Marcar una notificación propia como leída"),
            entry("PATCH /api/personal/notificaciones/leidas", "Marcar todas las notificaciones propias como leídas"),
            entry("GET /api/personal/pedidos", "Listar pedidos operativos o completados"),
            entry("GET /api/personal/pedidos/pagina", "Buscar pedidos por páginas"),
            entry("GET /api/personal/pedidos/pagados", "Consultar los pedidos pagados y su monto total"),
            entry("POST /api/personal/pedidos", "Registrar un pedido como personal"),
            entry("GET /api/personal/pedidos/{id}/historial", "Consultar el historial de un pedido"),
            entry("PATCH /api/personal/pedidos/{id}/entrega", "Actualizar la entrega de un pedido"),
            entry("PATCH /api/personal/pedidos/{id}/pago", "Actualizar el pago de un pedido"),
            entry("GET /api/personal/dashboard/resumen", "Consultar el resumen de pedidos operativos"),
            entry("GET /api/personal/reportes/completados", "Consultar el reporte de pedidos entregados y pagados"),
            entry("GET /api/personal/reportes/completados/csv", "Descargar el reporte de completados en CSV"));

    private static final Map<String, String> ORDENES_PERMITIDOS = Map.of(
            "/api/admin/usuarios/pagina", "id, nombreUsuario, nombre, apellido, correo, rol, activo",
            "/api/admin/empleados/pagina", "id, dni, fechaIngreso, nombreUsuario, nombre, apellido, correo, rol, activo",
            "/api/personal/pedidos/pagina",
            "id, numeroPedido, nombreCliente, telefonoCliente, fechaRegistro, fechaEntrega, estadoEntrega, estadoPago, origen");

    private static final Map<String, String> BUSQUEDAS = Map.of(
            "/api/admin/usuarios/pagina", "Busca por usuario, nombre, apellido o correo, sin distinguir mayúsculas.",
            "/api/admin/empleados/pagina", "Busca por DNI, usuario, nombre, apellido o correo, sin distinguir mayúsculas.",
            "/api/personal/pedidos/pagina",
            "Busca por nombre o teléfono del cliente, o por número de pedido completo como AQX-0001 o su identificador numérico.");

    @Bean
    OpenAPI aquaxOpenApi() {
        Components componentes = new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))
                .addSecuritySchemes("basicAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("basic"));
        registrarErrores(componentes);
        return new OpenAPI()
                .info(new Info().title("AquaX API").version("0.0.1")
                        .description("API de pedidos, empleados y administración de AquaX. "
                                + "Los endpoints protegidos admiten JWT Bearer o HTTP Basic. "
                                + "Las páginas se numeran desde cero."))
                .components(componentes)
                .tags(MODULOS.values().stream().distinct().sorted()
                        .map(nombre -> new Tag().name(nombre)).toList())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
    }

    @Bean
    OpenApiCustomizer documentarOperacionesAquax() {
        return api -> {
            if (api.getPaths() == null) {
                return;
            }
            api.getPaths().forEach((ruta, item) -> item.readOperationsMap().forEach((metodo, operacion) -> {
                if (ruta.startsWith("/api/")) {
                    documentarOperacion(ruta, metodo, operacion);
                }
            }));
            marcarContrasenasDeEntrada(api);
        };
    }

    private void documentarOperacion(String ruta, HttpMethod metodo, Operation operacion) {
        String modulo = MODULOS.entrySet().stream()
                .filter(entrada -> ruta.startsWith(entrada.getKey()))
                .map(Map.Entry::getValue).findFirst().orElse("AquaX");
        operacion.setTags(List.of(modulo));
        operacion.setSummary(RESUMENES.getOrDefault(metodo.name() + " " + ruta, operacion.getSummary()));
        if (operacion.getResponses() == null) {
            operacion.setResponses(new ApiResponses());
        }

        boolean publica = esPublica(ruta, metodo);
        if (publica) {
            operacion.setSecurity(new ArrayList<>());
        } else {
            agregarRespuesta(operacion, "401", "NoAutorizado");
            agregarRespuesta(operacion, "403", "Prohibido");
            operacion.setDescription(ruta.startsWith("/api/admin/")
                    ? "Requiere el rol Administrador."
                    : "Admite los roles Administrador y Vendedor.");
        }
        if (metodo != HttpMethod.GET || tieneParametros(operacion)) {
            agregarRespuesta(operacion, "400", "SolicitudInvalida");
        }
        if (ruta.contains("{id}") || ruta.equals("/api/admin/empresa")) {
            agregarRespuesta(operacion, "404", "NoEncontrado");
        }
        if (puedeGenerarConflicto(ruta, metodo)) {
            agregarRespuesta(operacion, "409", "Conflicto");
        }
        if (ruta.equals("/api/auth/login")) {
            agregarRespuesta(operacion, "401", "NoAutorizado");
            agregarRespuesta(operacion, "503", "JwtNoConfigurado");
        }
        agregarRespuesta(operacion, "500", "ErrorInterno");
        if (ruta.endsWith("/pagina")) {
            documentarPaginacion(ruta, operacion);
        }
        documentarFiltros(ruta, operacion);
        if (ruta.endsWith("/csv")) {
            documentarCsv(operacion);
        }
    }

    private boolean esPublica(String ruta, HttpMethod metodo) {
        return metodo == HttpMethod.GET && ruta.equals("/api/productos")
                || metodo == HttpMethod.POST && (ruta.equals("/api/pedidos") || ruta.equals("/api/auth/login"));
    }

    private boolean tieneParametros(Operation operacion) {
        return operacion.getParameters() != null && !operacion.getParameters().isEmpty();
    }

    private boolean puedeGenerarConflicto(String ruta, HttpMethod metodo) {
        return metodo == HttpMethod.POST && (ruta.startsWith("/api/admin/usuarios")
                || ruta.startsWith("/api/admin/empleados"))
                || metodo == HttpMethod.PUT && (ruta.startsWith("/api/admin/empleados")
                || ruta.equals("/api/personal/perfil"))
                || metodo == HttpMethod.PATCH && (ruta.startsWith("/api/admin/usuarios")
                || ruta.startsWith("/api/admin/empleados"));
    }

    private void agregarRespuesta(Operation operacion, String codigo, String referencia) {
        operacion.getResponses().addApiResponse(codigo,
                new ApiResponse().$ref("#/components/responses/" + referencia));
    }

    private void documentarPaginacion(String ruta, Operation operacion) {
        List<Parameter> parametros = new ArrayList<>();
        if (operacion.getParameters() != null) {
            operacion.getParameters().stream()
                    .filter(parametro -> !List.of("pageable", "page", "size", "sort").contains(parametro.getName()))
                    .forEach(parametros::add);
        }
        IntegerSchema pagina = new IntegerSchema();
        pagina.setMinimum(java.math.BigDecimal.ZERO);
        pagina.setDefault(0);
        IntegerSchema tamano = new IntegerSchema();
        tamano.setMinimum(java.math.BigDecimal.ONE);
        tamano.setMaximum(java.math.BigDecimal.valueOf(50));
        tamano.setDefault(10);
        parametros.add(new Parameter().name("page").in("query")
                .description("Número de página, comenzando en cero.").schema(pagina));
        parametros.add(new Parameter().name("size").in("query")
                .description("Cantidad de registros por página. Los valores superiores a 50 se limitan a 50.")
                .schema(tamano));
        parametros.add(new Parameter().name("sort").in("query")
                .description("Orden con formato campo,asc o campo,desc. Puede repetirse. "
                        + "Campos admitidos: " + ORDENES_PERMITIDOS.get(ruta)
                        + ". El identificador resuelve los empates de orden.")
                .style(Parameter.StyleEnum.FORM).explode(true)
                .schema(new ArraySchema().items(new StringSchema())
                        .example(List.of(ruta.contains("/pedidos/") ? "fechaRegistro,desc" : "nombre,asc"))));
        operacion.setParameters(parametros);
    }

    private void documentarFiltros(String ruta, Operation operacion) {
        if (operacion.getParameters() == null) {
            return;
        }
        for (Parameter parametro : operacion.getParameters()) {
            switch (parametro.getName()) {
                case "texto" -> parametro.setDescription(BUSQUEDAS.getOrDefault(ruta,
                        "Texto de búsqueda aplicado antes de calcular los totales de página."));
                case "completados" -> parametro.setDescription("true consulta entregados y pagados; false consulta pedidos operativos.");
                case "desde" -> parametro.setDescription("Fecha de entrega inicial incluida, con formato yyyy-MM-dd.");
                case "hasta" -> parametro.setDescription("Fecha de entrega final incluida, con formato yyyy-MM-dd.");
                case "soloNoLeidas" -> parametro.setDescription("true devuelve únicamente notificaciones propias sin leer.");
                default -> {
                }
            }
        }
    }

    private void documentarCsv(Operation operacion) {
        operacion.getResponses().addApiResponse("200", new ApiResponse()
                .description("CSV UTF-8 con BOM. Un rango vacío devuelve únicamente los encabezados.")
                .content(new Content().addMediaType("text/csv", new MediaType()
                        .schema(new StringSchema().format("binary"))))
                .addHeaderObject("Content-Disposition", new Header()
                        .description("Nombre sugerido del archivo de descarga.")
                        .schema(new StringSchema()).example("attachment; filename=\"aquax-historial-2026-11-11.csv\"")));
    }

    private void registrarErrores(Components componentes) {
        Schema<?> campo = new ObjectSchema();
        campo.addProperty("campo", new StringSchema());
        campo.addProperty("mensaje", new StringSchema());
        campo.setRequired(List.of("campo", "mensaje"));
        componentes.addSchemas("ErrorCampo", campo);
        Schema<?> problema = new ObjectSchema()
                .addProperty("type", new StringSchema().format("uri"))
                .addProperty("title", new StringSchema())
                .addProperty("status", new IntegerSchema())
                .addProperty("detail", new StringSchema())
                .addProperty("instance", new StringSchema().format("uri-reference"))
                .addProperty("errores", new ArraySchema().items(new Schema<>().$ref("#/components/schemas/ErrorCampo")));
        problema.setRequired(List.of("status", "title", "detail", "instance"));
        componentes.addSchemas("ProblemaApi", problema);

        componentes.addResponses("SolicitudInvalida", respuestaError(
                "JSON, parámetros o campos inválidos.", referenciaProblema()));
        componentes.addResponses("NoAutorizado", respuestaError(
                "Credenciales ausentes o incorrectas, cuenta inactiva o JWT inválido o vencido.",
                referenciaProblema()).addHeaderObject("WWW-Authenticate", new Header()
                        .description("Mecanismo de autenticación admitido o correspondiente a las credenciales enviadas.")
                        .schema(new StringSchema())));
        componentes.addResponses("Prohibido", respuestaError(
                "La cuenta autenticada carece del rol requerido.",
                referenciaProblema()));
        componentes.addResponses("NoEncontrado", respuestaError(
                "Recurso inexistente o notificación que no pertenece a la cuenta autenticada.", referenciaProblema()));
        componentes.addResponses("Conflicto", respuestaError(
                "Datos duplicados o una operación que dejaría al sistema sin administradores activos.", referenciaProblema()));
        componentes.addResponses("JwtNoConfigurado", respuestaError(
                "Configura AQUAX_JWT_SECRET para habilitar el acceso con JWT.", referenciaProblema()));
        componentes.addResponses("ErrorInterno", respuestaError(
                "Fallo inesperado. El mensaje público no expone detalles internos.", referenciaProblema()));
    }

    private Schema<?> referenciaProblema() {
        return new Schema<>().$ref("#/components/schemas/ProblemaApi");
    }

    private ApiResponse respuestaError(String descripcion, Schema<?> esquema) {
        return new ApiResponse().description(descripcion).content(new Content()
                .addMediaType("application/problem+json", new MediaType().schema(esquema)));
    }

    private void marcarContrasenasDeEntrada(OpenAPI api) {
        if (api.getComponents() == null || api.getComponents().getSchemas() == null) {
            return;
        }
        for (Schema<?> esquema : api.getComponents().getSchemas().values()) {
            if (esquema.getProperties() != null) {
                esquema.getProperties().forEach((nombre, propiedad) -> {
                    if (List.of("contrasena", "contrasenaActual", "nuevaContrasena", "confirmarContrasena").contains(nombre)) {
                        propiedad.setWriteOnly(true);
                        propiedad.setFormat("password");
                    }
                });
            }
        }
    }
}
