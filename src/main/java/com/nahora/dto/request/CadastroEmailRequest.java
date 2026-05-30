package com.nahora.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CadastroEmailRequest(
        @NotBlank(message = "O telefone é obrigatório")
        String telefone,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email
) {}
