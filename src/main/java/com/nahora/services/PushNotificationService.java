package com.nahora.services;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PushNotificationService {

    /**
     * Envia uma notificação Push via Firebase Cloud Messaging (FCM)
     */
    public void enviarNotificacao(String tokenDispositivo, String titulo, String corpo) {
        
        if (tokenDispositivo == null || tokenDispositivo.trim().isEmpty()) {
            log.warn("Não é possível enviar Push: Token do dispositivo está nulo.");
            return;
        }

        try {
            // Formata o corpo para não mandar uma mensagem gigante (corta em 100 caracteres)
            String corpoResumido = corpo.length() > 100 ? corpo.substring(0, 97) + "..." : corpo;

            // Monta a notificação padrão do Firebase
            Notification notification = Notification.builder()
                    .setTitle(titulo)
                    .setBody(corpoResumido)
                    .build();

            // Monta a mensagem apontando para o celular específico (token)
            Message message = Message.builder()
                    .setToken(tokenDispositivo)
                    .setNotification(notification)
                    .build();

            // Dispara para os servidores do Google
            String response = FirebaseMessaging.getInstance().send(message);
            
            log.info("Push enviado com sucesso! MessageID: {}", response);

        } catch (Exception e) {
            log.error("Falha ao enviar notificação Push via Firebase", e);
        }
    }
}