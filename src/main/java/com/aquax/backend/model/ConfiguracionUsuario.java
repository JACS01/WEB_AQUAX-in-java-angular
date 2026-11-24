package com.aquax.backend.model;

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
@Table(name = "configuraciones_usuario")
public class ConfiguracionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true, updatable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 10)
    private String idioma = "es";

    @Column(nullable = false)
    private int nivelZoom = 100;

    @Column(nullable = false, length = 30)
    private String filtroColor = "none";

    @Column(nullable = false)
    private boolean filtroColorActivo;

    protected ConfiguracionUsuario() {
    }

    public ConfiguracionUsuario(Usuario usuario) {
        this.usuario = Objects.requireNonNull(usuario);
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getIdioma() {
        return idioma;
    }

    public int getNivelZoom() {
        return nivelZoom;
    }

    public String getFiltroColor() {
        return filtroColor;
    }

    public boolean isFiltroColorActivo() {
        return filtroColorActivo;
    }

    public void actualizar(String idioma, int nivelZoom, String filtroColor, boolean activo) {
        this.idioma = Objects.requireNonNull(idioma);
        this.nivelZoom = nivelZoom;
        this.filtroColor = Objects.requireNonNull(filtroColor);
        this.filtroColorActivo = activo;
    }
}
