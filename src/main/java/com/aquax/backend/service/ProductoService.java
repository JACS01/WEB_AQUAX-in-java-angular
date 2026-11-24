package com.aquax.backend.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aquax.backend.dto.ProductoResponse;
import com.aquax.backend.repository.ProductoRepository;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar() {
        return productoRepository.findAll(Sort.by("id")).stream()
                .map(producto -> new ProductoResponse(
                        producto.getId(), producto.getNombre(),
                        producto.getDescripcion(), producto.getPrecio()))
                .toList();
    }
}
