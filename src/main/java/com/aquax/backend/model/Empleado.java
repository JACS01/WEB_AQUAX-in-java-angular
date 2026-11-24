package com.aquax.backend.model;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "empleados")
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true, updatable = false)
    private Usuario usuario;

    @Column(nullable = false, unique = true, length = 8)
    private String dni;

    @Column(nullable = false)
    private LocalDate fechaIngreso;

    protected Empleado() {
    }

    public Empleado(Usuario usuario, String dni, LocalDate fechaIngreso) {
        this.usuario = Objects.requireNonNull(usuario);
        actualizarDatosLaborales(dni, fechaIngreso);
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getDni() {
        return dni;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void actualizarDatosLaborales(String dni, LocalDate fechaIngreso) {
        this.dni = Objects.requireNonNull(dni).trim();
        this.fechaIngreso = Objects.requireNonNull(fechaIngreso);
    }
}
