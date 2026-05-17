package com.nahora.dto.response;

import com.nahora.model.enums.CategoriaServico;
import com.nahora.model.enums.StatusPedido;
import com.nahora.model.enums.Urgencia;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class PedidoResponse {

    private Long id;
    private Long clienteId;
    private String clienteNome;
    private CategoriaServico categoria;
    private String descricao;
    private List<String> fotos;
    private EnderecoResponse endereco;
    private Urgencia urgencia;
    private BigDecimal orcamentoEstimado;
    private LocalDateTime dataDesejada;
    private StatusPedido status;
    private LocalDateTime criadoEm;
    private Long profissionalAtribuidoId;
    private String profissionalAtribuidoNome;

}
