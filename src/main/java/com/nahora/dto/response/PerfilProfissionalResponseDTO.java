package com.nahora.dto.response;

import com.nahora.model.enums.CategoriaServico;
import com.nahora.model.enums.StatusVerificacao;
import java.util.List;

public record PerfilProfissionalResponseDTO(
        Long id,
        String nome,
        String foto,
        String bio,
        List<CategoriaServico> categorias,
        List<String> especialidades,
        Integer anosExperiencia,
        Double raioAtuacaoKm,
        Double notaMedia,
        Integer totalAvaliacoes,
        Integer totalServicosExecutados,
        List<String> portfolio,
        Boolean disponivel,
        StatusVerificacao statusVerificacao
) {
    
}

