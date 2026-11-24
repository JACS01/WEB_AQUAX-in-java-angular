package com.aquax.backend.controller;

import java.util.List;

import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.aquax.backend.config.ProblemasApi;
import com.aquax.backend.dto.ErrorCampoResponse;
import com.aquax.backend.service.ConflictoEmpleadoException;
import com.aquax.backend.service.ConflictoUsuarioException;
import com.aquax.backend.service.EmpleadoNoEncontradoException;
import com.aquax.backend.service.EmpresaNoEncontradaException;
import com.aquax.backend.service.JwtNoConfiguradoException;
import com.aquax.backend.service.NotificacionNoEncontradaException;
import com.aquax.backend.service.PedidoNoEncontradoException;
import com.aquax.backend.service.ProductoNoEncontradoException;
import com.aquax.backend.service.UsuarioNoEncontradoException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ValidacionControllerAdvice extends ResponseEntityExceptionHandler {

    private final ProblemasApi problemas;

    public ValidacionControllerAdvice(ProblemasApi problemas) {
        this.problemas = problemas;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException error, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        List<ErrorCampoResponse> errores = error.getBindingResult().getFieldErrors().stream()
                .map(campo -> new ErrorCampoResponse(campo.getField(), campo.getDefaultMessage()))
                .toList();
        ProblemDetail cuerpo = problemas.crear(status, "Revisa los campos indicados en errores.", peticion(request));
        cuerpo.setTitle("Datos de solicitud inválidos");
        cuerpo.setProperty("errores", errores);
        return super.handleExceptionInternal(error, cuerpo, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException error, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail cuerpo = problemas.crear(status,
                "El cuerpo debe ser un JSON válido con los tipos de datos esperados.", peticion(request));
        cuerpo.setTitle("Contenido de solicitud inválido");
        return super.handleExceptionInternal(error, cuerpo, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException error, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String campo = error instanceof MethodArgumentTypeMismatchException parametro
                ? parametro.getName() : "parametro";
        String mensaje = mensajeDeTipo(error.getRequiredType());
        ProblemDetail cuerpo = problemas.crear(status, "Revisa el formato del parámetro indicado en errores.", peticion(request));
        cuerpo.setProperty("errores", List.of(new ErrorCampoResponse(campo, mensaje)));
        return super.handleExceptionInternal(error, cuerpo, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException error, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail cuerpo = problemas.crear(status, "Falta un parámetro obligatorio de la solicitud.", peticion(request));
        cuerpo.setProperty("errores", List.of(new ErrorCampoResponse(error.getParameterName(), "El parámetro es obligatorio.")));
        return super.handleExceptionInternal(error, cuerpo, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception error, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (status.is5xxServerError()) {
            registrarFallo(error);
        }
        ProblemDetail cuerpo = problemas.crear(status, detalleGeneral(status), peticion(request));
        return super.handleExceptionInternal(error, cuerpo, headers, status, request);
    }

    @ExceptionHandler({EmpleadoNoEncontradoException.class, EmpresaNoEncontradaException.class,
            NotificacionNoEncontradaException.class, PedidoNoEncontradoException.class,
            UsuarioNoEncontradoException.class})
    public ProblemDetail noEncontrado(RuntimeException error, HttpServletRequest request) {
        return problemas.crear(HttpStatus.NOT_FOUND, error.getMessage(), request);
    }

    @ExceptionHandler(ProductoNoEncontradoException.class)
    public ProblemDetail productoNoEncontrado(ProductoNoEncontradoException error, HttpServletRequest request) {
        return problemas.crear(HttpStatus.BAD_REQUEST, error.getMessage(), request);
    }

    @ExceptionHandler({ConflictoEmpleadoException.class, ConflictoUsuarioException.class})
    public ProblemDetail conflicto(RuntimeException error, HttpServletRequest request) {
        return problemas.crear(HttpStatus.CONFLICT, error.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail datosEnConflicto(HttpServletRequest request) {
        return problemas.crear(HttpStatus.CONFLICT, "La operación entra en conflicto con los datos existentes.", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail datosInvalidos(IllegalArgumentException error, HttpServletRequest request) {
        return problemas.crear(HttpStatus.BAD_REQUEST, error.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> autenticacionIncorrecta(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(problemas.crear(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos.", request));
    }

    @ExceptionHandler(AuthenticationServiceException.class)
    public ProblemDetail falloDelServicioDeAutenticacion(
            AuthenticationServiceException error, HttpServletRequest request) {
        registrarFallo(error);
        return problemas.crear(HttpStatus.INTERNAL_SERVER_ERROR, null, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail accesoDenegado(HttpServletRequest request) {
        return problemas.crear(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta operación.", request);
    }

    @ExceptionHandler(JwtNoConfiguradoException.class)
    public ProblemDetail jwtNoConfigurado(JwtNoConfiguradoException error, HttpServletRequest request) {
        return problemas.crear(HttpStatus.SERVICE_UNAVAILABLE, error.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail errorNoControlado(Exception error, HttpServletRequest request) {
        registrarFallo(error);
        return problemas.crear(HttpStatus.INTERNAL_SERVER_ERROR, null, request);
    }

    private HttpServletRequest peticion(WebRequest request) {
        return ((ServletWebRequest) request).getRequest();
    }

    private String mensajeDeTipo(Class<?> tipo) {
        if (tipo == int.class || tipo == Integer.class || tipo == long.class || tipo == Long.class) {
            return "Debe ser un número entero válido.";
        }
        if (tipo == boolean.class || tipo == Boolean.class) {
            return "Debe ser true o false.";
        }
        if (tipo == java.time.LocalDate.class) {
            return "Debe ser una fecha válida con formato yyyy-MM-dd.";
        }
        return "El formato del parámetro no es válido.";
    }

    private String detalleGeneral(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "La solicitud contiene datos inválidos o incompletos.";
            case 404 -> "No se encontró el recurso solicitado.";
            case 405 -> "El método HTTP no está permitido para este recurso.";
            case 406 -> "El formato de respuesta solicitado no está disponible.";
            case 413 -> "El contenido de la solicitud supera el tamaño permitido.";
            case 415 -> "El tipo de contenido de la solicitud no está admitido.";
            case 503 -> "El servicio no está disponible temporalmente.";
            default -> "No se pudo procesar la solicitud.";
        };
    }

    private void registrarFallo(Exception error) {
        StackTraceElement[] origen = error.getStackTrace();
        logger.error("Error interno de tipo " + error.getClass().getName()
                + (origen.length == 0 ? "" : " en " + origen[0]));
    }
}
