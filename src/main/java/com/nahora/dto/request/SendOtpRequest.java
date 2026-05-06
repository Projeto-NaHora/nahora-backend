package com.nahora.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendOtpRequest(
        @NotBlank(message = "O telefone é obrigatório")
        @Size(min = 10, max = 15, message = "Telefone deve conter DDD e o número")
        String telefone
) {}