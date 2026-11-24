package com.aquax.backend.model;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ajustes_empresariales")
public class AjusteEmpresarial {

    @Id
    private Long id = 1L;

    @Column(nullable = false, length = 150)
    private String razonSocial;

    @Column(nullable = false, length = 11)
    private String ruc;

    @Column(nullable = false, length = 300)
    private String direccion;

    @Column(length = 15)
    private String telefono1;

    @Column(length = 15)
    private String telefono2;

    @Column(length = 200)
    private String correo;

    @Column(length = 500)
    private String registroSanitario;

    @Column(length = 200)
    private String slogan;

    @Column(nullable = false)
    private LocalDate ultimaActualizacion;

    protected AjusteEmpresarial() {
    }

    public AjusteEmpresarial(String razonSocial, String ruc, String direccion,
                             String telefono1, String telefono2, String correo,
                             String registroSanitario, String slogan, LocalDate fecha) {
        actualizar(razonSocial, ruc, direccion, telefono1, telefono2, correo,
                registroSanitario, slogan, fecha);
    }

    public void actualizar(String razonSocial, String ruc, String direccion,
                           String telefono1, String telefono2, String correo,
                           String registroSanitario, String slogan, LocalDate fecha) {
        this.razonSocial = Objects.requireNonNull(razonSocial).trim();
        this.ruc = Objects.requireNonNull(ruc).trim();
        this.direccion = Objects.requireNonNull(direccion).trim();
        this.telefono1 = normalizarOpcional(telefono1);
        this.telefono2 = normalizarOpcional(telefono2);
        this.correo = normalizarOpcional(correo);
        this.registroSanitario = normalizarOpcional(registroSanitario);
        this.slogan = normalizarOpcional(slogan);
        this.ultimaActualizacion = Objects.requireNonNull(fecha);
    }

    private String normalizarOpcional(String valor) {
        return valor == null ? null : valor.trim();
    }

    public Long getId() {
        return id;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public String getRuc() {
        return ruc;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getTelefono1() {
        return telefono1;
    }

    public String getTelefono2() {
        return telefono2;
    }

    public String getCorreo() {
        return correo;
    }

    public String getRegistroSanitario() {
        return registroSanitario;
    }

    public String getSlogan() {
        return slogan;
    }

    public LocalDate getUltimaActualizacion() {
        return ultimaActualizacion;
    }
}
