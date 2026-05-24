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
public class PedidoPublicoResponse {

    private Long id;
    private CategoriaServico categoria;
    private String descricao;
    private Urgencia urgencia;
    private BigDecimal orcamentoEstimado;
    private StatusPedido status;
    private LocalDateTime criadoEm;
    private String bairro;
    private String cidade;
    private String clienteNome;
    private List<String> fotos;
    private LocalDateTime dataDesejada;
}
