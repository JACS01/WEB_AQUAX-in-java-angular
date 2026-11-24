package com.aquax.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aquax.backend.model.ConfiguracionUsuario;

public interface ConfiguracionUsuarioRepository extends JpaRepository<ConfiguracionUsuario, Long> {

    Optional<ConfiguracionUsuario> findByUsuario_Id(Long usuarioId);
}
