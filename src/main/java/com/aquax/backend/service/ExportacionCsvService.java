package com.aquax.backend.service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.aquax.backend.dto.PedidoResponse;

@Service
public class ExportacionCsvService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] exportarCompletados(List<PedidoResponse> pedidos) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("N Pedido,Cliente,Telefono,Producto,Cantidad,Origen,Pago,Fecha\r\n");

        for (PedidoResponse pedido : pedidos) {
            String productos = pedido.detalles().isEmpty()
                    ? pedido.cantidadProducto() + " unidades"
                    : pedido.detalles().stream()
                            .map(detalle -> detalle.nombreProducto() + " x" + detalle.cantidad())
                            .collect(Collectors.joining(", "));
            String fecha = pedido.fechaEntrega() == null ? "" : pedido.fechaEntrega().format(FORMATO_FECHA);
            Object[] valores = {
                    pedido.numeroPedido(), pedido.nombreCliente(), pedido.telefonoCliente(),
                    productos, pedido.cantidadProducto(), pedido.origen(), pedido.estadoPago(), fecha
            };

            for (int columna = 0; columna < valores.length; columna++) {
                if (columna > 0) {
                    csv.append(',');
                }
                csv.append(escaparCelda(valores[columna]));
            }
            csv.append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escaparCelda(Object valor) {
        String texto = valor == null ? "" : valor.toString();
        if (puedeInterpretarseComoFormula(texto)) {
            texto = "'" + texto;
        }
        return "\"" + texto.replace("\"", "\"\"") + "\"";
    }

    private boolean puedeInterpretarseComoFormula(String texto) {
        int inicio = 0;
        while (inicio < texto.length()
                && (Character.isWhitespace(texto.charAt(inicio)) || Character.isSpaceChar(texto.charAt(inicio)))) {
            char caracter = texto.charAt(inicio);
            if (caracter == '\t' || caracter == '\r' || caracter == '\n') {
                return true;
            }
            inicio++;
        }
        return inicio < texto.length() && "=+-@".indexOf(texto.charAt(inicio)) >= 0;
    }
}
