package com.nahora.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "O identificador é obrigatório")
        String identificador
) {}
