package com.nahora.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MensagemRequestDTO(
     @NotBlank(message = "O conteúdo da mensagem não pode estar vazio")
        String conteudo,
        String anexoUrl // Opcional
) {}
