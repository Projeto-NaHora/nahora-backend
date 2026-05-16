package com.nahora.controllers;

import com.nahora.dto.request.PropostaRequestDTO;
import com.nahora.dto.response.PropostaResponse;
import com.nahora.model.Profissional;
import com.nahora.model.Usuario;
import com.nahora.services.PropostaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/pedidos/{pedidoId}/propostas")
@RequiredArgsConstructor
@Tag(name = "Propostas", description = "Formalização de proposta pelo profissional")
public class PropostaController {

    private final PropostaService propostaService;

    @PostMapping
    @Operation(summary = "Cria ou atualiza uma proposta para um pedido (upsert)",
            description = "Cria a proposta caso o profissional ainda não tenha uma ativa para o pedido, ou atualiza a existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Proposta criada com sucesso"),
            @ApiResponse(responseCode = "200", description = "Proposta atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Campos inválidos (valor negativo, horário com fim ≤ início ou horários sobrepostos)"),
            @ApiResponse(responseCode = "403", description = "Profissional tentando enviar proposta para pedido próprio"),
            @ApiResponse(responseCode = "422", description = "Pedido não está com status ABERTO ou cliente já aceitou outra proposta")
    })
    public ResponseEntity<PropostaResponse> enviarProposta(
            @PathVariable Long pedidoId,
            @Valid @RequestBody PropostaRequestDTO request,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {

        if (!(usuarioAutenticado instanceof Profissional profissional)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas profissionais podem enviar propostas.");
        }

        PropostaService.PropostaSalvaResult resultado =
                propostaService.salvarOuAtualizarProposta(pedidoId, profissional.getId(), request);

        HttpStatus status = resultado.criada() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(propostaService.toResponseDTO(resultado.proposta()));
    }
}
