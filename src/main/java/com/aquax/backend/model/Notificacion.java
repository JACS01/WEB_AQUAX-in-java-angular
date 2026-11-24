package com.aquax.backend.model;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "notificaciones", uniqueConstraints = {
        @UniqueConstraint(name = "uk_notificacion_pedido_usuario", columnNames = {"pedido_id", "usuario_id"})
})
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, updatable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 200, updatable = false)
    private String titulo;

    @Column(nullable = false, length = 500, updatable = false)
    private String mensaje;

    @Column(nullable = false, length = 50, updatable = false)
    private String tipo;

    @Column(nullable = false)
    private boolean leida;

    @Column(nullable = false, updatable = false)
    private Instant fechaCreacion;

    protected Notificacion() {
    }

    public Notificacion(Pedido pedido, Usuario usuario, Instant fechaCreacion) {
        this.pedido = Objects.requireNonNull(pedido);
        this.usuario = Objects.requireNonNull(usuario);
        this.fechaCreacion = Objects.requireNonNull(fechaCreacion);
        this.titulo = "Nuevo pedido de cliente";
        this.mensaje = "Nuevo pedido " + pedido.getNumeroPedido() + " de " + pedido.getNombreCliente() + ".";
        this.tipo = "pedido_nuevo";
        this.leida = false;
    }

    public Long getId() {
        return id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getTipo() {
        return tipo;
    }

    public boolean isLeida() {
        return leida;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void marcarComoLeida() {
        leida = true;
    }
}
