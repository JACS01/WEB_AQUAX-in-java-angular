package com.aquax.backend.service;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aquax.backend.dto.ActualizarEmpresaRequest;
import com.aquax.backend.dto.EmpresaResponse;
import com.aquax.backend.model.AjusteEmpresarial;
import com.aquax.backend.repository.AjusteEmpresarialRepository;

@Service
public class AjusteEmpresarialService {

    private static final Long ID_EMPRESA = 1L;
    private static final ZoneId ZONA_OPERACION = ZoneId.of("America/Lima");

    private final AjusteEmpresarialRepository ajusteRepository;

    public AjusteEmpresarialService(AjusteEmpresarialRepository ajusteRepository) {
        this.ajusteRepository = ajusteRepository;
    }

    @Transactional(readOnly = true)
    public EmpresaResponse consultar() {
        AjusteEmpresarial ajuste = ajusteRepository.findById(ID_EMPRESA)
                .orElseThrow(EmpresaNoEncontradaException::new);
        return aRespuesta(ajuste);
    }

    @Transactional
    public EmpresaResponse actualizar(ActualizarEmpresaRequest solicitud) {
        AjusteEmpresarial ajuste = ajusteRepository.buscarParaActualizar(ID_EMPRESA)
                .orElseThrow(EmpresaNoEncontradaException::new);
        ajuste.actualizar(solicitud.razonSocial(), solicitud.ruc(), solicitud.direccion(),
                solicitud.telefono1(), solicitud.telefono2(), solicitud.correo(),
                solicitud.registroSanitario(), solicitud.slogan(), LocalDate.now(ZONA_OPERACION));
        return aRespuesta(ajuste);
    }

    private EmpresaResponse aRespuesta(AjusteEmpresarial ajuste) {
        return new EmpresaResponse(ajuste.getId(), ajuste.getRazonSocial(), ajuste.getRuc(),
                ajuste.getDireccion(), ajuste.getTelefono1(), ajuste.getTelefono2(), ajuste.getCorreo(),
                ajuste.getRegistroSanitario(), ajuste.getSlogan(), ajuste.getUltimaActualizacion());
    }
}
