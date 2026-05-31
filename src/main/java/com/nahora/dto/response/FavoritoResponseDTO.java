package com.nahora.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record FavoritoResponseDTO(
        Long profissionalId,
        String nomeProfissional,
        String fotoProfissional,
        Double mediaAvaliacao,
        Integer totalAvaliacoes,
        List<String> categorias,
        LocalDateTime favoritadoEm
) {}
