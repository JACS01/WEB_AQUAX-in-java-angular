package com.aquax.backend.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombreCliente;

    @Column(nullable = false, length = 15)
    private String telefonoCliente;

    @Column(nullable = false, length = 300)
    private String direccionEntrega;

    @Column(length = 500)
    private String observaciones;

    @Column(nullable = false)
    private LocalDate fechaRegistro;

    private LocalDate fechaEntrega;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigenPedido origen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_registro_id", updatable = false)
    private Usuario usuarioRegistro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEntrega estadoEntrega;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estadoPago;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new ArrayList<>();

    protected Pedido() {
    }

    public Pedido(String nombreCliente, String telefonoCliente, String direccionEntrega,
                  String observaciones, LocalDate fechaRegistro, OrigenPedido origen) {
        this.nombreCliente = nombreCliente;
        this.telefonoCliente = telefonoCliente;
        this.direccionEntrega = direccionEntrega;
        this.observaciones = observaciones;
        this.fechaRegistro = fechaRegistro;
        this.origen = origen;
        this.estadoEntrega = EstadoEntrega.NUEVO;
        this.estadoPago = EstadoPago.PENDIENTE;
    }

    public Pedido(String nombreCliente, String telefonoCliente, String direccionEntrega,
                  String observaciones, LocalDate fechaRegistro, Usuario usuarioRegistro) {
        this(nombreCliente, telefonoCliente, direccionEntrega, observaciones, fechaRegistro,
                OrigenPedido.TRABAJADOR);
        this.usuarioRegistro = Objects.requireNonNull(usuarioRegistro, "El usuario de registro es obligatorio.");
    }

    public void agregarDetalle(Producto producto, int cantidad) {
        detalles.add(new DetallePedido(this, producto, cantidad));
    }

    public void cambiarEstadoEntrega(EstadoEntrega nuevo, LocalDate fecha) {
        Objects.requireNonNull(nuevo, "El estado de entrega es obligatorio.");
        if (estadoEntrega == nuevo) {
            return;
        }
        fechaEntrega = nuevo == EstadoEntrega.ENTREGADO
                ? Objects.requireNonNull(fecha, "La fecha de entrega es obligatoria.")
                : null;
        estadoEntrega = nuevo;
    }

    public void cambiarEstadoPago(EstadoPago nuevo) {
        estadoPago = Objects.requireNonNull(nuevo, "El estado de pago es obligatorio.");
    }

    public Long getId() {
        return id;
    }

    public String getNumeroPedido() {
        return id == null ? null : String.format(Locale.ROOT, "AQX-%04d", id);
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public String getTelefonoCliente() {
        return telefonoCliente;
    }

    public String getDireccionEntrega() {
        return direccionEntrega;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public LocalDate getFechaEntrega() {
        return fechaEntrega;
    }

    public OrigenPedido getOrigen() {
        return origen;
    }

    public Usuario getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public EstadoEntrega getEstadoEntrega() {
        return estadoEntrega;
    }

    public EstadoPago getEstadoPago() {
        return estadoPago;
    }

    public List<DetallePedido> getDetalles() {
        return List.copyOf(detalles);
    }

    public long getCantidadProducto() {
        return detalles.stream().mapToLong(DetallePedido::getCantidad).sum();
    }

    public BigDecimal getTotal() {
        return detalles.stream().map(DetallePedido::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
