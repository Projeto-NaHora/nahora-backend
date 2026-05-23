package com.nahora.controllers;

import com.nahora.dto.request.MensagemRequestDTO;
import com.nahora.services.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat/{conversaId}/enviar")
    public void enviarMensagem(@DestinationVariable Long conversaId, MensagemRequestDTO dto) {
    }
}