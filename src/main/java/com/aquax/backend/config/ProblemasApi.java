package com.aquax.backend.config;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
public class ProblemasApi {

    private final ObjectMapper mapper;

    public ProblemasApi(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public ProblemDetail crear(HttpStatusCode estado, String detalle, HttpServletRequest solicitud) {
        return normalizar(ProblemDetail.forStatusAndDetail(estado, detalle), solicitud);
    }

    public ProblemDetail normalizar(ProblemDetail problema, HttpServletRequest solicitud) {
        int estado = problema.getStatus();
        HttpStatus http = HttpStatus.resolve(estado);
        String tituloActual = problema.getTitle();
        if (estado == 500 || tituloActual == null || tituloActual.isBlank()
                || http != null && tituloActual.equals(http.getReasonPhrase())) {
            problema.setTitle(titulo(estado));
        }
        if (estado == 500 || problema.getDetail() == null || problema.getDetail().isBlank()) {
            problema.setDetail(detalle(estado));
        }
        String ruta = solicitud.getRequestURI();
        try {
            problema.setInstance(URI.create(ruta));
        } catch (IllegalArgumentException error) {
            problema.setInstance(URI.create("/"));
        }
        return problema;
    }

    public void escribir(HttpServletRequest solicitud, HttpServletResponse respuesta,
                         HttpStatusCode estado, String detalle) throws IOException {
        if (respuesta.isCommitted()) {
            return;
        }
        respuesta.resetBuffer();
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        mapper.writeValue(respuesta.getOutputStream(), crear(estado, detalle, solicitud));
    }

    private String titulo(int estado) {
        return switch (estado) {
            case 400 -> "Solicitud inválida";
            case 401 -> "No autorizado";
            case 403 -> "Acceso denegado";
            case 404 -> "Recurso no encontrado";
            case 405 -> "Método no permitido";
            case 406 -> "Formato de respuesta no admitido";
            case 409 -> "Conflicto";
            case 413 -> "Solicitud demasiado grande";
            case 415 -> "Tipo de contenido no admitido";
            case 422 -> "Datos no procesables";
            case 429 -> "Demasiadas solicitudes";
            case 500 -> "Error interno del servidor";
            case 501 -> "Operación no disponible";
            case 502 -> "Error del servidor de destino";
            case 503 -> "Servicio no disponible";
            case 504 -> "Tiempo de espera agotado";
            default -> "Error en la solicitud";
        };
    }

    private String detalle(int estado) {
        return switch (estado) {
            case 400 -> "La solicitud contiene datos o parámetros inválidos.";
            case 401 -> "Debes autenticarte con credenciales válidas.";
            case 403 -> "No tienes permisos para realizar esta operación.";
            case 404 -> "No se encontró el recurso solicitado.";
            case 405 -> "El método HTTP no está permitido para esta ruta.";
            case 406 -> "No se puede generar una respuesta en el formato solicitado.";
            case 409 -> "La operación entra en conflicto con los datos actuales.";
            case 413 -> "La solicitud supera el tamaño admitido.";
            case 415 -> "El tipo de contenido de la solicitud no está admitido.";
            case 429 -> "Se recibieron demasiadas solicitudes. Inténtalo más tarde.";
            case 503 -> "El servicio no está disponible temporalmente.";
            default -> "Ocurrió un error interno. Inténtalo nuevamente más tarde.";
        };
    }
}
