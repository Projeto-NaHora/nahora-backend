package com.nahora.model;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Embeddable
@Getter @Setter
public class JanelaHorario {
    private LocalDateTime inicio;
    private LocalDateTime fim;
}