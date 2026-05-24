package com.nahora.services;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.nahora.model.Pedido;
import com.nahora.model.Profissional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("prod")
public class FirebasePushNotificationService implements PushNotificationService {

    @Override
    public void enviarNotificacaoNovoPedido(Profissional profissional, Pedido pedido) {
        log.warn("FirebasePushNotificationService: profissional {} sem fcmToken — push ignorado até o campo ser adicionado ao modelo",
                profissional.getId());
    }

    @Override
    public void enviarNotificacao(String token, String titulo, String corpo) {
        enviar(token, titulo, corpo);
    }

    private void enviar(String token, String titulo, String corpo) {
        if (token == null || token.isBlank()) {
            log.warn("Token FCM nulo ou vazio — push ignorado");
            return;
        }
        String corpoResumido = corpo.length() > 100 ? corpo.substring(0, 97) + "..." : corpo;
        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(titulo)
                            .setBody(corpoResumido)
                            .build())
                    .build();
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Push enviado com sucesso | MessageID: {}", response);
        } catch (Exception e) {
            log.error("Falha ao enviar push via Firebase", e);
        }
    }
}
