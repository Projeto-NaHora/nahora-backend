package com.nahora.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "O identificador é obrigatório")
        String identificador,

        @NotBlank(message = "A senha é obrigatória")
        String senha
) {}
