package com.nahora.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ProfissionalDocumentosRequest(
        @NotBlank(message = "A URL do RG (frente) é obrigatória")
        String rgFrente,

        @NotBlank(message = "A URL do RG (verso) é obrigatória")
        String rgVerso,

        @NotBlank(message = "A URL da selfie com documento é obrigatória")
        String selfieComDocumento
) {}
