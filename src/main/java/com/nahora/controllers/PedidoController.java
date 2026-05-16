package com.nahora.controllers;

import com.nahora.dto.request.PedidoRequest;
import com.nahora.dto.response.PedidoResponse;
import com.nahora.model.Cliente;
import com.nahora.model.Pedido;
import com.nahora.model.Usuario;
import com.nahora.services.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.nahora.dto.response.AceitarPropostaResponseDTO;
import com.nahora.dto.response.PropostaResponseDTO;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "Gerenciamento de pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    @Operation(summary = "Cria um pedido associado a um cliente")
    public ResponseEntity<PedidoResponse> criarPedido(
            @Valid @RequestBody PedidoRequest request,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        if (!(usuarioAutenticado instanceof Cliente clienteAutenticado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas clientes podem criar pedidos.");
        }
        Pedido novoPedido = pedidoService.criarPedido(clienteAutenticado.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.toResponseDTO(novoPedido));
    }
    @GetMapping("/{pedidoId}/propostas")
    @Operation(summary = "Lista as propostas ativas/pendentes recebidas para um pedido (Tela C05)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Usuário autenticado não é o dono do pedido"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    public ResponseEntity<List<PropostaResponseDTO>> listarPropostas(
            @PathVariable Long pedidoId,
            @RequestParam(required = false) String ordenarPor,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {

        if (!(usuarioAutenticado instanceof Cliente clienteAutenticado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas clientes podem visualizar propostas de pedidos.");
        }

        List<PropostaResponseDTO> propostas = pedidoService.listarPropostasAtivas(pedidoId, clienteAutenticado.getId(), ordenarPor);
        return ResponseEntity.ok(propostas);
    }

    @PostMapping("/{pedidoId}/propostas/{propostaId}/aceitar")
    @Operation(summary = "Aceita uma proposta e atribui o pedido ao profissional escolhido (UC-09)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Aceite realizado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Usuário autenticado não é o dono do pedido"),
            @ApiResponse(responseCode = "404", description = "Pedido ou proposta não encontrados"),
            @ApiResponse(responseCode = "422", description = "Pedido não está com status ABERTO")
    })
    public ResponseEntity<AceitarPropostaResponseDTO> aceitarProposta(
            @PathVariable Long pedidoId,
            @PathVariable Long propuestaId,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {

        if (!(usuarioAutenticado instanceof Cliente clienteAutenticado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas clientes podem aceitar propostas.");
        }

        AceitarPropostaResponseDTO response = pedidoService.aceitarProposta(pedidoId, propuestaId, clienteAutenticado.getId());
        return ResponseEntity.ok(response);
    }
}
