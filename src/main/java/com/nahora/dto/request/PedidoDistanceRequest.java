package com.nahora.dto.request;

import com.nahora.model.Pedido;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PedidoDistanceRequest {
    private final Pedido pedido;
    private final Double distanciaKm;
}
