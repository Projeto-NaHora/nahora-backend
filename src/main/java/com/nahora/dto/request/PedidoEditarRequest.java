package com.nahora.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoEditarRequest(
        @NotBlank(message = "Descrição não pode estar em branco")
        @Size(min = 20, max = 500, message = "Descrição deve ter no mínimo 20 e no máximo 500 caracteres")
        @Schema(
                description = "Nova descrição detalhada do serviço. Mínimo de 20 e máximo de 500 caracteres.",
                example = "Meu chuveiro elétrico fez um cheiro de queimado e agora preciso trocar a resistência e fiação.",
                minLength = 20,
                maxLength = 500,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String descricao,

        @Size(max = 5, message = "Máximo de 5 fotos permitidas")
        List<@NotBlank String> fotos,

        @Future(message = "Data desejada deve ser futura")
        LocalDateTime dataDesejada
) {}