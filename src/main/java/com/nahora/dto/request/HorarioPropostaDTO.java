package com.nahora.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class HorarioPropostaDTO {

    @NotNull(message = "O horário de início é obrigatório")
    private LocalDateTime inicio;

    @NotNull(message = "O horário de fim é obrigatório")
    private LocalDateTime fim;
}
