package com.aquax.backend.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.aquax.backend.model.AjusteEmpresarial;
import com.aquax.backend.model.Producto;
import com.aquax.backend.repository.AjusteEmpresarialRepository;
import com.aquax.backend.repository.ProductoRepository;

@Configuration(proxyBeanMethods = false)
@Profile("sqlite")
public class DatosInicialesConfig {

    @Bean
    ApplicationRunner crearDatosIniciales(
            ProductoRepository productoRepository,
            AjusteEmpresarialRepository ajusteRepository,
            PlatformTransactionManager transactionManager) {
        return args -> {
            TransactionTemplate transaccion = new TransactionTemplate(transactionManager);
            transaccion.executeWithoutResult(status -> {
                if (productoRepository.count() == 0) {
                    productoRepository.saveAll(List.of(
                            new Producto("Bidón 20L", "Bidón retornable de 20 litros", new BigDecimal("10.00")),
                            new Producto("Paquete 15 x 500 ml", "Paquete de 15 botellas de 500 ml",
                                    new BigDecimal("12.00")),
                            new Producto("Caja 20L", "Caja de 20 litros", new BigDecimal("17.00"))));
                }
                if (!ajusteRepository.existsById(1L)) {
                    ajusteRepository.save(new AjusteEmpresarial(
                            "AQUAX", "10715736689", "Porcon Carretera San Pablo",
                            "916154315", "955265327", null, "P0663225N FACBSR",
                            "La Diferencia es el Origen", LocalDate.of(2026, 1, 1)));
                }
            });
        };
    }
}
