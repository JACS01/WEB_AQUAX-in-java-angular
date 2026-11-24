package com.aquax.backend.model;

import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombreUsuario;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    @Column(length = 15)
    private String telefono;

    @Column(nullable = false, length = 100)
    private String contrasena;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RolUsuario rol;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long versionToken;

    protected Usuario() {
    }

    public Usuario(String nombreUsuario, String nombre, String apellido, String correo,
                   String contrasena, RolUsuario rol) {
        this.nombreUsuario = Objects.requireNonNull(nombreUsuario).trim().toLowerCase(Locale.ROOT);
        this.nombre = Objects.requireNonNull(nombre).trim();
        this.apellido = Objects.requireNonNull(apellido).trim();
        this.correo = Objects.requireNonNull(correo).trim().toLowerCase(Locale.ROOT);
        this.contrasena = Objects.requireNonNull(contrasena);
        this.rol = Objects.requireNonNull(rol);
    }

    public Long getId() {
        return id;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getCorreo() {
        return correo;
    }

    public String getTelefono() {
        return telefono;
    }

    @JsonIgnore
    public String getContrasena() {
        return contrasena;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void cambiarActivo(boolean activo) {
        this.activo = activo;
    }

    public void cambiarRol(RolUsuario rol) {
        this.rol = Objects.requireNonNull(rol);
    }

    public void actualizarPerfil(String nombre, String apellido, String correo, String telefono) {
        this.nombre = Objects.requireNonNull(nombre).trim();
        this.apellido = Objects.requireNonNull(apellido).trim();
        this.correo = Objects.requireNonNull(correo).trim().toLowerCase(Locale.ROOT);
        this.telefono = Objects.requireNonNull(telefono).trim();
    }

    public void cambiarContrasena(String contrasenaCodificada) {
        this.contrasena = Objects.requireNonNull(contrasenaCodificada);
        this.versionToken++;
    }

    @JsonIgnore
    public long getVersionToken() {
        return versionToken;
    }
}
