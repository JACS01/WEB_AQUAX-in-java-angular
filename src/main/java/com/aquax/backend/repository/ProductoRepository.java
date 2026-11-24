package com.aquax.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aquax.backend.model.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
}
