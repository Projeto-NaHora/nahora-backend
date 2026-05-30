package com.nahora.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CadastroSenhaRequest(
        @NotBlank(message = "O telefone é obrigatório")
        String telefone,

        @NotBlank(message = "A senha é obrigatória")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,}$",
                message = "A senha deve ter no mínimo 8 caracteres, contendo pelo menos uma letra e um número"
        )
        String senha,

        @NotBlank(message = "A confirmação de senha é obrigatória")
        String confirmacaoSenha
) {}
