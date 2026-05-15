package com.nahora.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
public class PropostaRequestDTO {

    @NotNull(message = "O valor oferecido é obrigatório")
    @Positive(message = "O valor oferecido deve ser positivo")
    private BigDecimal valorOferecido;

    private String descricao;

    @NotNull(message = "Os horários disponíveis são obrigatórios")
    @Size(min = 1, max = 3, message = "Informe entre 1 e 3 horários disponíveis")
    private List<@Valid HorarioPropostaDTO> horariosDisponiveis;
}
