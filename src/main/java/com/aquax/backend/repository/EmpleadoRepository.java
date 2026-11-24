package com.aquax.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aquax.backend.model.Empleado;

import jakarta.persistence.LockModeType;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    @EntityGraph(attributePaths = "usuario")
    @Query("""
            select empleado from Empleado empleado join empleado.usuario usuario
            where empleado.dni like :patron escape '!'
               or lower(usuario.nombreUsuario) like :patron escape '!'
               or lower(usuario.nombre) like :patron escape '!'
               or lower(usuario.apellido) like :patron escape '!'
               or lower(usuario.correo) like :patron escape '!'
            """)
    Page<Empleado> buscarPagina(@Param("patron") String patron, Pageable pagina);

    @Override
    @EntityGraph(attributePaths = "usuario")
    List<Empleado> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = "usuario")
    Optional<Empleado> findById(Long id);

    boolean existsByUsuario_Id(Long usuarioId);

    boolean existsByDni(String dni);

    boolean existsByDniAndIdNot(String dni, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select empleado from Empleado empleado where empleado.id = :id")
    Optional<Empleado> buscarParaActualizar(@Param("id") Long id);

    @Query("select empleado.usuario.id from Empleado empleado where empleado.id = :id")
    Optional<Long> buscarUsuarioId(@Param("id") Long id);
}
