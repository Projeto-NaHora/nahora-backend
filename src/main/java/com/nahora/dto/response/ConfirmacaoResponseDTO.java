package com.nahora.dto.response;

import com.nahora.model.enums.StatusPedido;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ConfirmacaoResponseDTO {
    private Long pedidoId;
    private StatusPedido status;
    private LocalDateTime concluidoEm;
    private LocalDateTime prazoAutoConfirmacaoEm;
    private String fotoConclusaoUrl;
}
