package com.nahora.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminVerificarRequest(
        @NotNull(message = "O campo 'aprovado' é obrigatório")
        Boolean aprovado
) {}
