package com.nahora.dto.response;

import java.time.LocalDateTime;

public record AdminProfissionalPendenteDTO(
        Long id,
        String nome,
        String telefone,
        String email,
        String rgFrente,
        String rgVerso,
        String selfieComDocumento,
        LocalDateTime criadoEm
) {}
