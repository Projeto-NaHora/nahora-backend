package com.nahora.dto.request;

import com.nahora.model.enums.CategoriaServico;
import com.nahora.model.enums.Urgencia;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PedidoRequest {

    @NotNull(message = "Categoria é obrigatória")
    private CategoriaServico categoria;

    @NotBlank(message = "Descrição não pode estar em branco")
    @Size(min = 20, max = 500, message = "Descrição deve ter no mínimo 20 e no máximo 500 caracteres")
    @Schema(
            description = "Descrição detalhada do serviço que o cliente precisa. Mínimo de 20 e máximo de 500 caracteres.",
            example = "Meu chuveiro elétrico fez um cheiro de queimado e não esquenta mais.",
            minLength = 20,
            maxLength = 500,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String descricao;

    private Integer enderecoSalvoIndex;

    // Se não existir endereço salvo
    @Valid
    private EnderecoRequest endereco;

    @Size(max = 5, message = "Máximo de 5 fotos permitidas")
    private List<@NotBlank String> fotos = new ArrayList<>();

    @NotNull(message = "Urgência é obrigatória")
    private Urgencia urgencia;

    @PositiveOrZero(message = "Orçamento estimado deve ser zero ou positivo")
    private BigDecimal orcamentoEstimado;

    @Future(message = "Data desejada deve ser futura")
    @NotNull(message = "Data desejada é obrigatória")
    private LocalDateTime dataDesejada;
}