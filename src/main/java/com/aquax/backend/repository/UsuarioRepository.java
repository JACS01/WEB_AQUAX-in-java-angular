package com.aquax.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aquax.backend.model.RolUsuario;
import com.aquax.backend.model.Usuario;

import jakarta.persistence.LockModeType;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("""
            select usuario from Usuario usuario
            where lower(usuario.nombreUsuario) like :patron escape '!'
               or lower(usuario.nombre) like :patron escape '!'
               or lower(usuario.apellido) like :patron escape '!'
               or lower(usuario.correo) like :patron escape '!'
            """)
    Page<Usuario> buscarPagina(@Param("patron") String patron, Pageable pagina);

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    boolean existsByNombreUsuario(String nombreUsuario);

    boolean existsByCorreo(String correo);

    boolean existsByCorreoAndIdNot(String correo, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select usuario from Usuario usuario where usuario.nombreUsuario = :nombreUsuario")
    Optional<Usuario> buscarPorNombreParaActualizar(@Param("nombreUsuario") String nombreUsuario);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select usuario from Usuario usuario where usuario.id = :id")
    Optional<Usuario> buscarParaActualizar(@Param("id") Long id);

    List<Usuario> findByActivoTrueAndRolIn(List<RolUsuario> roles);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select usuario from Usuario usuario where usuario.rol = :rol and usuario.activo = true order by usuario.id")
    List<Usuario> bloquearAdministradoresActivos(@Param("rol") RolUsuario rol);
}
