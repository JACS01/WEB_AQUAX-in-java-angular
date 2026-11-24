package com.aquax.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aquax.backend.model.AjusteEmpresarial;

import jakarta.persistence.LockModeType;

public interface AjusteEmpresarialRepository extends JpaRepository<AjusteEmpresarial, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ajuste from AjusteEmpresarial ajuste where ajuste.id = :id")
    Optional<AjusteEmpresarial> buscarParaActualizar(@Param("id") Long id);
}
