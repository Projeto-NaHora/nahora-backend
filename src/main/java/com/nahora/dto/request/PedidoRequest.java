package com.nahora.dto.request;

import com.nahora.model.enums.CategoriaServico;
import com.nahora.model.enums.Urgencia;
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
    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
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