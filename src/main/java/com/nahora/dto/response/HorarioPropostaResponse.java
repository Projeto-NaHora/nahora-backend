package com.nahora.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class HorarioPropostaResponse {

    private Long id;
    private LocalDateTime inicio;
    private LocalDateTime fim;
}
