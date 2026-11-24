package com.aquax.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.aquax.backend.model.EstadoEntrega;
import com.aquax.backend.model.EstadoPago;
import com.aquax.backend.model.OrigenPedido;
import com.aquax.backend.model.Pedido;
import com.aquax.backend.model.Producto;

class PedidoCalculosTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 11);

    @Test
    void sumaImportesDecimalesYUnidadesDeTodosLosDetalles() {
        Pedido pedido = nuevoPedido();
        pedido.agregarDetalle(producto("Botella", "0.10"), 3);
        pedido.agregarDetalle(producto("Paquete", "0.20"), 2);

        assertThat(pedido.getCantidadProducto()).isEqualTo(5L);
        assertThat(pedido.getTotal()).isEqualByComparingTo("0.70");
        assertThat(pedido.getDetalles()).hasSize(2);
        assertThat(pedido.getDetalles().getFirst().getSubtotal()).isEqualByComparingTo("0.30");
    }

    @Test
    void rechazaCantidadesNoPositivasSinAgregarDetalles() {
        Pedido pedido = nuevoPedido();
        Producto producto = producto("Botella", "12.35");

        assertThatIllegalArgumentException().isThrownBy(() -> pedido.agregarDetalle(producto, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> pedido.agregarDetalle(producto, -1));
        assertThat(pedido.getDetalles()).isEmpty();
        assertThat(pedido.getCantidadProducto()).isZero();
        assertThat(pedido.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void acumulaCantidadesGrandesSinDesbordarEnterosNiPerderCentimos() {
        Pedido pedido = nuevoPedido();
        Producto producto = producto("Botella", "0.10");
        pedido.agregarDetalle(producto, Integer.MAX_VALUE);
        pedido.agregarDetalle(producto, Integer.MAX_VALUE);

        assertThat(pedido.getCantidadProducto()).isEqualTo(4_294_967_294L);
        assertThat(pedido.getTotal()).isEqualByComparingTo("429496729.40");
    }

    @Test
    void entregaYPagoCambianDeFormaIndependiente() {
        Pedido pedido = nuevoPedido();
        pedido.cambiarEstadoPago(EstadoPago.PAGADO);
        assertThat(pedido.getEstadoEntrega()).isEqualTo(EstadoEntrega.NUEVO);
        assertThat(pedido.getFechaEntrega()).isNull();

        pedido.cambiarEstadoEntrega(EstadoEntrega.ENTREGADO, FECHA);
        assertThat(pedido.getFechaEntrega()).isEqualTo(FECHA);
        assertThat(pedido.getEstadoPago()).isEqualTo(EstadoPago.PAGADO);

        pedido.cambiarEstadoPago(EstadoPago.PENDIENTE);
        assertThat(pedido.getEstadoEntrega()).isEqualTo(EstadoEntrega.ENTREGADO);
        assertThat(pedido.getFechaEntrega()).isEqualTo(FECHA);
    }

    @Test
    void repetirEntregaConservaSuFechaYReabrirLaLimpia() {
        Pedido pedido = nuevoPedido();
        pedido.cambiarEstadoEntrega(EstadoEntrega.ENTREGADO, FECHA);
        pedido.cambiarEstadoEntrega(EstadoEntrega.ENTREGADO, FECHA.plusDays(1));
        assertThat(pedido.getFechaEntrega()).isEqualTo(FECHA);

        pedido.cambiarEstadoEntrega(EstadoEntrega.EN_CAMINO, null);
        assertThat(pedido.getEstadoEntrega()).isEqualTo(EstadoEntrega.EN_CAMINO);
        assertThat(pedido.getFechaEntrega()).isNull();
    }

    @Test
    void noPermiteMarcarEntregaSinFechaNiCambiarAUnEstadoNulo() {
        Pedido pedido = nuevoPedido();

        assertThatNullPointerException()
                .isThrownBy(() -> pedido.cambiarEstadoEntrega(EstadoEntrega.ENTREGADO, null));
        assertThatNullPointerException().isThrownBy(() -> pedido.cambiarEstadoEntrega(null, FECHA));
        assertThatNullPointerException().isThrownBy(() -> pedido.cambiarEstadoPago(null));
        assertThat(pedido.getEstadoEntrega()).isEqualTo(EstadoEntrega.NUEVO);
        assertThat(pedido.getEstadoPago()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(pedido.getFechaEntrega()).isNull();
    }

    private Pedido nuevoPedido() {
        return new Pedido("Cliente", "999888777", "Dirección", null, FECHA, OrigenPedido.CLIENTE);
    }

    private Producto producto(String nombre, String precio) {
        return new Producto(nombre, "Producto de prueba", new BigDecimal(precio));
    }
}
