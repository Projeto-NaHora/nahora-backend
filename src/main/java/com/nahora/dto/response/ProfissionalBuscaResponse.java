package com.nahora.dto.response;

import java.util.List;

public record ProfissionalBuscaResponse(
        List<ProfissionalCardDTO> profissionais,
        long totalEncontrados,
        int paginaAtual,
        int totalPaginas
) {}
