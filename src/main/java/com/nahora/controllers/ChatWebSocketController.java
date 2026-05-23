package com.nahora.controllers;

import com.nahora.dto.request.MensagemRequestDTO;
import com.nahora.model.Usuario;
import com.nahora.repositories.UsuarioRepository;
import com.nahora.services.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;


@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final UsuarioRepository usuarioRepository;

   @MessageMapping("/chat/{conversaId}/enviar")
    public void enviarMensagem(@DestinationVariable Long conversaId,
                               MensagemRequestDTO request,
                               Authentication authentication) {
        
        // Pega o e-mail do principal autenticado
        String userEmail = ((org.springframework.security.core.userdetails.User) authentication.getPrincipal()).getUsername();

        // Busca o seu Usuario real no banco
        Usuario usuarioAutenticado = usuarioRepository.findByEmail(userEmail)
        .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        // Chama o service UMA ÚNICA VEZ com as variáveis corretas
        chatService.enviarMensagem(conversaId, usuarioAutenticado.getId(), request);
    }
}