package com.aquax.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aquax.backend.dto.ActualizarEmpresaRequest;
import com.aquax.backend.dto.EmpresaResponse;
import com.aquax.backend.service.AjusteEmpresarialService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/empresa")
public class AjusteEmpresarialController {

    private final AjusteEmpresarialService ajusteService;

    public AjusteEmpresarialController(AjusteEmpresarialService ajusteService) {
        this.ajusteService = ajusteService;
    }

    @GetMapping
    public EmpresaResponse consultar() {
        return ajusteService.consultar();
    }

    @PutMapping
    public EmpresaResponse actualizar(@Valid @RequestBody ActualizarEmpresaRequest solicitud) {
        return ajusteService.actualizar(solicitud);
    }

}
