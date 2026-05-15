package com.nahora.dto.response;

import com.nahora.model.enums.StatusProposta;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
public class PropostaResponse {

    private Long id;
    private Long pedidoId;
    private Long profissionalId;
    private BigDecimal valorOferecido;
    private String descricao;
    private StatusProposta status;
    private List<HorarioPropostaResponse> horariosDisponiveis;
    private LocalDateTime criadoEm;
}
