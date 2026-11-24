package com.aquax.backend.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aquax.backend.dto.CambiarEstadoEntregaRequest;
import com.aquax.backend.dto.CambiarEstadoPagoRequest;
import com.aquax.backend.dto.CrearPedidoRequest;
import com.aquax.backend.dto.HistorialModificacionResponse;
import com.aquax.backend.dto.HistorialPagadosResponse;
import com.aquax.backend.dto.PedidoResponse;
import com.aquax.backend.dto.PaginaResponse;
import com.aquax.backend.service.PedidoService;
import com.aquax.backend.service.Paginacion;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/personal/pedidos")
public class PedidoPersonalController {

    private final PedidoService pedidoService;

    public PedidoPersonalController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    public List<PedidoResponse> listar(@RequestParam(defaultValue = "false") boolean completados) {
        return pedidoService.listarParaPersonal(completados);
    }

    @GetMapping("/pagina")
    public PaginaResponse<PedidoResponse> listarPagina(
            @RequestParam(defaultValue = "false") boolean completados,
            @RequestParam(required = false) String texto,
            @PageableDefault(size = 10, sort = "fechaRegistro", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Paginacion.validarParametros(page, size);
        return pedidoService.listarPagina(completados, texto, pageable);
    }

    @GetMapping("/pagados")
    public HistorialPagadosResponse consultarPagados() {
        return pedidoService.consultarPagados();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse registrar(@Valid @RequestBody CrearPedidoRequest solicitud, Principal usuario) {
        return pedidoService.registrarPedidoPersonal(solicitud, usuario.getName());
    }

    @GetMapping("/{id}/historial")
    public List<HistorialModificacionResponse> consultarHistorial(@PathVariable Long id) {
        return pedidoService.consultarHistorial(id);
    }

    @PatchMapping("/{id}/entrega")
    public PedidoResponse cambiarEntrega(@PathVariable Long id,
                                         @Valid @RequestBody CambiarEstadoEntregaRequest solicitud,
                                         Principal usuario) {
        return pedidoService.cambiarEntrega(id, solicitud.estadoEntrega(), usuario.getName());
    }

    @PatchMapping("/{id}/pago")
    public PedidoResponse cambiarPago(@PathVariable Long id,
                                      @Valid @RequestBody CambiarEstadoPagoRequest solicitud,
                                      Principal usuario) {
        return pedidoService.cambiarPago(id, solicitud.estadoPago(), usuario.getName());
    }

}
