package com.nahora.dto.response;

import com.nahora.model.enums.StatusPedido;

public record AceitarPropostaResponseDTO(
        Long pedidoId,
        StatusPedido status,
        Long profissionalAtribuidoId,
        String profissionalNome
) {}