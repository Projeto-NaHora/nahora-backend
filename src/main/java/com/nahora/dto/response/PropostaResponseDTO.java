package com.nahora.dto.response;

import com.nahora.model.enums.StatusProposta;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PropostaResponseDTO(
        Long id,
        String profissionalNome,
        String profissionalFotoUrl,
        Double notaMedia,
        Integer numeroAvaliacoes,
        Integer numeroServicosRealizados,
        Double distanciaKm,
        String mensagem,
        BigDecimal valorProposto,
        List<JanelaHorarioDTO> horariosDisponiveis,
        StatusProposta status
) {
    public record JanelaHorarioDTO(
            LocalDateTime inicio,
            LocalDateTime fim
    ) {}
}