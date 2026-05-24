package com.nahora.services;

import com.nahora.dto.request.CategoriaDTO;
import com.nahora.repositories.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public List<CategoriaDTO> listarCategorias() {
        return categoriaRepository.findAllByOrderByNomeAsc().stream()
                .map(c -> CategoriaDTO.builder()
                        .id(c.getCategoriaServico().name())
                        .nome(c.getNome())
                        .icone(c.getIcone())
                        .build())
                .toList();
    }
}
