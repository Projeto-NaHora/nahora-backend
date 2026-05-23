package com.nahora.services;

import com.nahora.model.Pedido;
import com.nahora.model.Profissional;
import com.nahora.services.PushNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("dev")
public class LogPushNotificationService implements PushNotificationService {

    @Override
    public void enviarNotificacaoNovoPedido(Profissional profissional, Pedido pedido) {
        log.info("Disparando push de novo pedido. Profissional: {} (ID: {}) | Pedido ID: {}",
                profissional.getNome(), profissional.getId(), pedido.getId());
    }
}
