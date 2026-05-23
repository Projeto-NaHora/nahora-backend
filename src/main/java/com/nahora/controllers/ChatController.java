package com.nahora.controllers;

import com.nahora.dto.response.ConversaResponseDTO;
import com.nahora.dto.response.MensagemResponseDTO;
import com.nahora.model.Usuario;
import com.nahora.services.ChatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Endpoints REST para histórico e listagem de conversas")
public class ChatController {

    private final ChatService chatService;
    
}