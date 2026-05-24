package com.nahora.controllers;

import com.nahora.dto.response.ConversaResponseDTO;
import com.nahora.dto.response.MensagemResponseDTO;
import com.nahora.model.Usuario;
import com.nahora.model.enums.StatusConversa;
import com.nahora.services.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Endpoints REST para histórico e listagem de conversas")
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/conversas")
    @Operation(summary = "Listar conversas do usuário logado com filtros opcionais de status", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de conversas retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Usuário não autenticado ou sem permissão")
    })
    public ResponseEntity<Page<ConversaResponseDTO>> listarConversas(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @RequestParam(value = "status", required = false) List<StatusConversa> statusFiltro,
            Pageable pageable) {

        log.info("[HTTP] Requisição para listar conversas do usuário: {}", usuarioLogado.getId());

        List<StatusConversa> filtros = (statusFiltro == null || statusFiltro.isEmpty())
                ? java.util.List.of(StatusConversa.ABERTA, StatusConversa.SOMENTE_LEITURA, StatusConversa.EM_DISPUTA, StatusConversa.FECHADA)
                : statusFiltro;

        Page<ConversaResponseDTO> conversas = chatService.listarConversas(usuarioLogado.getId(), filtros, pageable);
        return ResponseEntity.ok(conversas);
    }

    @GetMapping("/conversas/{conversaId}/mensagens")
    @Operation(summary = "Buscar histórico paginado de mensagens de um canal de chat", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Usuário não é participante da conversa"),
            @ApiResponse(responseCode = "404", description = "Conversa não encontrada")
    })
    public ResponseEntity<Page<MensagemResponseDTO>> buscarHistorico(
            @PathVariable Long conversaId,
            @AuthenticationPrincipal Usuario usuarioLogado,
            Pageable pageable) {

        log.info("[HTTP] Requisição para histórico da conversa {} pelo usuário {}", conversaId, usuarioLogado.getId());

        Page<MensagemResponseDTO> historico = chatService.buscarHistorico(conversaId, usuarioLogado.getId(), pageable);
        return ResponseEntity.ok(historico);
    }

    @GetMapping("/propostas/{propostaId}/conversa")
    @Operation(summary = "Buscar os dados da conversa vinculada a uma proposta", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados da conversa retornados com sucesso"),
            @ApiResponse(responseCode = "403", description = "Usuário não é participante da conversa desta proposta"),
            @ApiResponse(responseCode = "404", description = "Nenhuma conversa encontrada para a proposta informada")
    })
    public ResponseEntity<ConversaResponseDTO> buscarConversaDaProposta(
            @PathVariable Long propostaId,
            @AuthenticationPrincipal Usuario usuarioLogado) {

        log.info("[HTTP] Buscando conversa da proposta {} para o usuário {}", propostaId, usuarioLogado.getId());

        ConversaResponseDTO conversaDto = chatService.buscarConversaDaProposta(propostaId, usuarioLogado.getId());
        return ResponseEntity.ok(conversaDto);
    }
}