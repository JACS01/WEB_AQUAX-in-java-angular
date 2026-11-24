package com.aquax.backend.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "historial_modificaciones")
public class HistorialModificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, updatable = false)
    private Pedido pedido;

    @Column(nullable = false, length = 255, updatable = false)
    private String usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", updatable = false)
    private Usuario cuentaUsuario;

    @Column(nullable = false, length = 30, updatable = false)
    private String campo;

    @Column(nullable = false, length = 20, updatable = false)
    private String valorAnterior;

    @Column(nullable = false, length = 20, updatable = false)
    private String valorNuevo;

    @Column(nullable = false, updatable = false)
    private Instant fecha;

    protected HistorialModificacion() {
    }

    public HistorialModificacion(
            Pedido pedido, String usuario, String campo, String valorAnterior, String valorNuevo, Instant fecha) {
        this.pedido = pedido;
        this.usuario = usuario;
        this.campo = campo;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
        this.fecha = fecha;
    }

    public HistorialModificacion(
            Pedido pedido, Usuario usuario, String campo, String valorAnterior, String valorNuevo, Instant fecha) {
        this(pedido, usuario.getNombreUsuario(), campo, valorAnterior, valorNuevo, fecha);
        this.cuentaUsuario = usuario;
    }

    public Long getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
    }

    public Usuario getCuentaUsuario() {
        return cuentaUsuario;
    }

    public String getCampo() {
        return campo;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public String getValorNuevo() {
        return valorNuevo;
    }

    public Instant getFecha() {
        return fecha;
    }
}
