package com.nahora.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nahora.model.Pedido;
import com.nahora.model.enums.CategoriaServico;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
public record PedidoResumoResponse(
        Long id,
        String descricao,
        CategoriaServico categoria,
        Double distanciaKm,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        LocalDateTime dataPublicacao,
        Boolean urgente,
        Double faixaValorMin,
        Double faixaValorMax,
        Integer contadorPropostas
) {

    private static final Map<CategoriaServico, ValorRange> FAIXA_VALOR_POR_CATEGORIA = Map.of(
            CategoriaServico.ELETRICA, new ValorRange(80.0, 250.0),
            CategoriaServico.PEDREIRO, new ValorRange(120.0, 400.0),
            CategoriaServico.ENCANAMENTO, new ValorRange(90.0, 300.0),
            CategoriaServico.PINTURA, new ValorRange(100.0, 350.0),
            CategoriaServico.MARCENARIA, new ValorRange(120.0, 600.0)
    );

    public static PedidoResumoResponse fromPedido(Pedido pedido, Double distanciaKm, Integer numPropostas) {
        ValorRange range = pedido.getCategoria() != null
                ? FAIXA_VALOR_POR_CATEGORIA.get(pedido.getCategoria())
                : null;

        return PedidoResumoResponse.builder()
                .id(pedido.getId())
                .descricao(pedido.getDescricao())
                .categoria(pedido.getCategoria())
                .distanciaKm(distanciaKm)
                .dataPublicacao(pedido.getCriadoEm())
                .urgente(pedido.getUrgente())
                .faixaValorMin(range != null ? range.min() : 0.0)
                .faixaValorMax(range != null ? range.max() : 0.0)
                .contadorPropostas(numPropostas)
                .build();
    }

    private record ValorRange(double min, double max) {}
}