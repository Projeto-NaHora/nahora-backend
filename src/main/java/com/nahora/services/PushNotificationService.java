package com.nahora.services;

import com.nahora.model.Pedido;
import com.nahora.model.Profissional;

public interface PushNotificationService {
    void enviarNotificacaoNovoPedido(Profissional profissional, Pedido pedido);
    void enviarNotificacao(String token, String titulo, String corpo);
}