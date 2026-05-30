package com.nahora.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CadastroNomeRequest(
        @NotBlank(message = "O telefone é obrigatório")
        String telefone,

        @NotBlank(message = "O nome é obrigatório")
        String nome,

        @NotBlank(message = "O sobrenome é obrigatório")
        String sobrenome
) {}
