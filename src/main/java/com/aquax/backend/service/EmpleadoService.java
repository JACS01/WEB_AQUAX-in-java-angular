package com.aquax.backend.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aquax.backend.dto.ActualizarEmpleadoRequest;
import com.aquax.backend.dto.ActualizarPerfilRequest;
import com.aquax.backend.dto.CrearEmpleadoConUsuarioRequest;
import com.aquax.backend.dto.CrearEmpleadoRequest;
import com.aquax.backend.dto.EmpleadoResponse;
import com.aquax.backend.dto.PaginaResponse;
import com.aquax.backend.dto.UsuarioResponse;
import com.aquax.backend.model.Empleado;
import com.aquax.backend.model.RolUsuario;
import com.aquax.backend.model.Usuario;
import com.aquax.backend.repository.EmpleadoRepository;
import com.aquax.backend.repository.UsuarioRepository;

@Service
public class EmpleadoService {

    private static final Map<String, String> CAMPOS_ORDEN = Map.of(
            "id", "id", "dni", "dni", "fechaIngreso", "fechaIngreso",
            "nombreUsuario", "usuario.nombreUsuario", "nombre", "usuario.nombre",
            "apellido", "usuario.apellido", "correo", "usuario.correo",
            "rol", "usuario.rol", "activo", "usuario.activo");

    private static final ZoneId ZONA_OPERACION = ZoneId.of("America/Lima");

    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    public EmpleadoService(EmpleadoRepository empleadoRepository,
                           UsuarioRepository usuarioRepository, UsuarioService usuarioService) {
        this.empleadoRepository = empleadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
    }

    @Transactional(readOnly = true)
    public List<EmpleadoResponse> listar() {
        return empleadoRepository.findAll(Sort.by("usuario.nombre", "usuario.apellido", "id"))
                .stream().map(this::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public PaginaResponse<EmpleadoResponse> listarPagina(String texto, Pageable pagina) {
        Pageable segura = Paginacion.segura(pagina, CAMPOS_ORDEN);
        return PaginaResponse.de(empleadoRepository.buscarPagina(Paginacion.patron(texto), segura)
                .map(this::aRespuesta));
    }

    @Transactional(readOnly = true)
    public EmpleadoResponse consultar(Long id) {
        return aRespuesta(buscarEmpleado(id));
    }

    @Transactional
    public void eliminar(Long id) {
        Empleado empleado = empleadoRepository.buscarParaActualizar(id)
                .orElseThrow(EmpleadoNoEncontradoException::new);
        empleadoRepository.delete(empleado);
        empleadoRepository.flush();
    }

    @Transactional
    public EmpleadoResponse registrar(CrearEmpleadoRequest solicitud) {
        Usuario usuario = usuarioRepository.buscarParaActualizar(solicitud.usuarioId())
                .orElseThrow(UsuarioNoEncontradoException::new);
        return vincular(usuario, solicitud.dni(), solicitud.fechaIngreso());
    }

    @Transactional
    public EmpleadoResponse registrarConUsuario(CrearEmpleadoConUsuarioRequest solicitud) {
        comprobarDniDisponible(solicitud.dni());
        UsuarioResponse cuenta = usuarioService.crear(solicitud.usuario());
        usuarioService.actualizarPerfil(cuenta.nombreUsuario(), new ActualizarPerfilRequest(
                cuenta.nombre(), cuenta.apellido(), cuenta.correo(), solicitud.telefono()));
        Usuario usuario = usuarioRepository.buscarParaActualizar(cuenta.id())
                .orElseThrow(UsuarioNoEncontradoException::new);
        return vincular(usuario, solicitud.dni(), solicitud.fechaIngreso());
    }

    @Transactional
    public EmpleadoResponse actualizar(Long id, ActualizarEmpleadoRequest solicitud) {
        Empleado empleado = empleadoRepository.buscarParaActualizar(id)
                .orElseThrow(EmpleadoNoEncontradoException::new);
        if (empleadoRepository.existsByDniAndIdNot(solicitud.dni(), id)) {
            throw new ConflictoEmpleadoException("El DNI ya está registrado por otro empleado.");
        }
        empleado.actualizarDatosLaborales(solicitud.dni(), solicitud.fechaIngreso());
        return aRespuesta(empleadoRepository.saveAndFlush(empleado));
    }

    @Transactional
    public EmpleadoResponse cambiarActivo(Long id, boolean activo) {
        Long usuarioId = empleadoRepository.buscarUsuarioId(id)
                .orElseThrow(EmpleadoNoEncontradoException::new);
        usuarioService.cambiarActivo(usuarioId, activo);
        return aRespuesta(buscarEmpleado(id));
    }

    @Transactional
    public EmpleadoResponse actualizarPerfil(Long id, ActualizarPerfilRequest solicitud) {
        Long usuarioId = empleadoRepository.buscarUsuarioId(id)
                .orElseThrow(EmpleadoNoEncontradoException::new);
        Usuario usuario = usuarioRepository.buscarParaActualizar(usuarioId)
                .orElseThrow(UsuarioNoEncontradoException::new);
        usuarioService.actualizarPerfil(usuario.getNombreUsuario(), solicitud);
        return aRespuesta(buscarEmpleado(id));
    }

    @Transactional
    public EmpleadoResponse cambiarRol(Long id, RolUsuario nuevoRol) {
        Long usuarioId = empleadoRepository.buscarUsuarioId(id)
                .orElseThrow(EmpleadoNoEncontradoException::new);
        usuarioService.cambiarRol(usuarioId, nuevoRol);
        return aRespuesta(buscarEmpleado(id));
    }

    private EmpleadoResponse vincular(Usuario usuario, String dni, LocalDate fechaIngreso) {
        if (empleadoRepository.existsByUsuario_Id(usuario.getId())) {
            throw new ConflictoEmpleadoException("La cuenta ya está vinculada a un empleado.");
        }
        comprobarDniDisponible(dni);
        LocalDate ingreso = fechaIngreso == null ? LocalDate.now(ZONA_OPERACION) : fechaIngreso;
        return aRespuesta(empleadoRepository.saveAndFlush(new Empleado(usuario, dni, ingreso)));
    }

    private void comprobarDniDisponible(String dni) {
        if (empleadoRepository.existsByDni(dni)) {
            throw new ConflictoEmpleadoException("El DNI ya está registrado por otro empleado.");
        }
    }

    private Empleado buscarEmpleado(Long id) {
        return empleadoRepository.findById(id).orElseThrow(EmpleadoNoEncontradoException::new);
    }

    private EmpleadoResponse aRespuesta(Empleado empleado) {
        Usuario usuario = empleado.getUsuario();
        return new EmpleadoResponse(empleado.getId(), empleado.getDni(), empleado.getFechaIngreso(),
                usuario.getId(), usuario.getNombreUsuario(), usuario.getNombre(), usuario.getApellido(),
                usuario.getCorreo(), usuario.getTelefono(), usuario.getRol(), usuario.isActivo());
    }
}
