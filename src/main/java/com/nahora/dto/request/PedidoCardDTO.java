package com.nahora.dto.request;

import com.nahora.model.enums.StatusPedido;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoCardDTO {

    private Long id;

    private String titulo;

    private StatusPedido status;

    private String data;

    private String periodo;

    private String profissionalNome;

    private String descricao;

    private String endereco;

}