package com.nahora.dto.request;

import com.nahora.model.enums.CategoriaServico;
import jakarta.validation.constraints.NotNull;

public record ProfissionalCategoriaRequest(
        @NotNull(message = "A categoria é obrigatória")
        CategoriaServico categoria
) {}
