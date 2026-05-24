package com.nahora.dto.response;

import java.util.List;

public record ProfissionalPerfilDTO(
        Long id,
        String nome,
        String fotoPerfil,
        Boolean badgePlus,
        Boolean disponivel,
        String categoriaNome,
        String cidade,
        Double mediaAvaliacoes,
        Integer totalAvaliacoes,
        Integer anosExperiencia,
        Integer totalServicos,
        String especialidadesDescricao,
        List<String> especialidadesTags,
        String sobreDescricao,
        List<String> portfolioFotos,
        Integer totalPortfolioFotos
) {}
