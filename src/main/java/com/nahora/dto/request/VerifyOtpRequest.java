package com.nahora.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyOtpRequest(
        @NotBlank(message = "O telefone é obrigatório")
        String telefone,

        @NotBlank(message = "O código OTP é obrigatório")
        @Size(min = 6, max = 6, message = "O código deve ter 6 dígitos")
        String codigo
) {}