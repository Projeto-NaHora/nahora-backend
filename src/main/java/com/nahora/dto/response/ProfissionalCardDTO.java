package com.nahora.dto.response;

public record ProfissionalCardDTO(
        Long id,
        String nome,
        String fotoPerfil,
        Boolean badgePlus,
        String categoriaNome,
        String cidade,
        Double distanciaKm,
        Double mediaAvaliacoes,
        Integer totalAvaliacoes
) {}
