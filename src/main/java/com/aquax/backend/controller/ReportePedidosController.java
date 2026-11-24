package com.aquax.backend.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aquax.backend.dto.ReporteCompletadosResponse;
import com.aquax.backend.service.ExportacionCsvService;
import com.aquax.backend.service.PedidoService;

@RestController
@RequestMapping("/api/personal/reportes")
public class ReportePedidosController {

    private final PedidoService pedidoService;
    private final ExportacionCsvService exportacionCsvService;

    public ReportePedidosController(PedidoService pedidoService, ExportacionCsvService exportacionCsvService) {
        this.pedidoService = pedidoService;
        this.exportacionCsvService = exportacionCsvService;
    }

    @GetMapping("/completados")
    public ReporteCompletadosResponse consultarCompletados(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return pedidoService.consultarCompletados(desde, hasta);
    }

    @GetMapping("/completados/csv")
    public ResponseEntity<byte[]> exportarCompletados(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        ReporteCompletadosResponse reporte = pedidoService.consultarCompletados(desde, hasta);
        String archivo = "aquax-historial-" + LocalDate.now(ZoneId.of("America/Lima")) + ".csv";
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo).build().toString())
                .body(exportacionCsvService.exportarCompletados(reporte.pedidos()));
    }

}
