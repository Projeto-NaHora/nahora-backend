package com.nahora.service;

import com.nahora.dto.request.CategoriaDTO;
import com.nahora.model.enums.CategoriaServico;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoriaService {


    public List<CategoriaDTO> listarCategorias() {
        return Arrays.stream(CategoriaServico.values())
                .map(categoria -> CategoriaDTO.builder()
                        .id(categoria.name())
                        .nome(categoria.getNome())
                        .icone(categoria.getIcone())
                        .build())
                .sorted(Comparator.comparing(CategoriaDTO::getNome)) // Ordena por nome
                .collect(Collectors.toList());
    }
}