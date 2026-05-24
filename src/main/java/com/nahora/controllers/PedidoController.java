package com.nahora.controllers;

import com.nahora.dto.request.PedidoFiltroRequest;
import com.nahora.dto.request.PedidoRequest;
import com.nahora.dto.response.PedidoResponse;
import com.nahora.dto.response.PedidoResumoResponse;
import com.nahora.dto.response.PedidoPublicoResponse;
import com.nahora.model.Cliente;
import com.nahora.model.Pedido;
import com.nahora.model.Usuario;
import com.nahora.model.enums.StatusPedido;
import com.nahora.services.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @Operation(summary = "Cria um pedido associado a um cliente", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PedidoResponse> criarPedido(
            @Valid @RequestBody PedidoRequest request,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        if (!(usuarioAutenticado instanceof Cliente clienteAutenticado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas clientes podem criar pedidos.");
        }
        Pedido novoPedido = pedidoService.criarPedido(clienteAutenticado.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.toResponseDTO(novoPedido));
    }

    @GetMapping("/{pedidoId}")
    @Operation(summary = "Retorna os detalhes de um pedido (Tela C04)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalhes retornados com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado ao pedido"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    public ResponseEntity<PedidoResponse> buscarPedido(
            @PathVariable Long pedidoId,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        return ResponseEntity.ok(pedidoService.buscarPedidoPorId(pedidoId, usuarioAutenticado));
    }

    @GetMapping("/{pedidoId}/public")
    @Operation(summary = "Retorna detalhes públicos de um pedido (sem informações sensíveis)",
            description = "Endpoint público. Não requer autenticação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalhes públicos retornados"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    public ResponseEntity<PedidoPublicoResponse> buscarPedidoPublico(
            @PathVariable Long pedidoId) {
        return ResponseEntity.ok(pedidoService.buscarPedidoPublicoPorId(pedidoId));
    }

    @DeleteMapping("/{pedidoId}")
    @Operation(summary = "Cancela um pedido do cliente autenticado",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido cancelado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Usuário não é o dono do pedido"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
            @ApiResponse(responseCode = "422", description = "Pedido não está com status ABERTO")
    })
    public ResponseEntity<PedidoResponse> cancelarPedido(
            @PathVariable Long pedidoId,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {

        if (!(usuarioAutenticado instanceof Cliente clienteAutenticado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas clientes podem cancelar pedidos.");
        }

        return ResponseEntity.ok(pedidoService.cancelarPedido(pedidoId, clienteAutenticado.getId()));
    }

    @PutMapping("/{pedidoId}")
    @Operation(summary = "Atualiza um pedido do cliente autenticado",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido atualizado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Usuário não é o dono do pedido"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
            @ApiResponse(responseCode = "422", description = "Pedido não está com status ABERTO")
    })
    public ResponseEntity<PedidoResponse> atualizarPedido(
            @PathVariable Long pedidoId,
            @Valid @RequestBody PedidoRequest request,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {

        if (!(usuarioAutenticado instanceof Cliente clienteAutenticado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas clientes podem editar pedidos.");
        }

        return ResponseEntity.ok(pedidoService.atualizarPedido(pedidoId, clienteAutenticado.getId(), request));
    }

    @GetMapping("/meus")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Lista os pedidos do cliente autenticado",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Usuário autenticado não é cliente")
    })
    public ResponseEntity<Page<PedidoResponse>> listarMeusPedidos(
            @RequestParam(required = false) List<StatusPedido> status,
            Pageable pageable,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        if (!(usuarioAutenticado instanceof Cliente clienteAutenticado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas clientes podem acessar seus pedidos.");
        }
        Page<PedidoResponse> pedidos = pedidoService.listarMeusPedidos(clienteAutenticado.getId(), status, pageable);
        return ResponseEntity.ok(pedidos);
    }

    @GetMapping("/disponiveis")
    @PreAuthorize("hasRole('PROFISSIONAL')")
    @Operation(summary = "Listar pedidos disponíveis para o profissional",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Page<PedidoResumoResponse>> listarPedidosParaProfissional(
            @ModelAttribute PedidoFiltroRequest filtro,
            Pageable pageable) {
        Page<PedidoResumoResponse> page = pedidoService.listarPedidosComFiltros(filtro, pageable);
        return ResponseEntity.ok(page);
    } // <--- CHAVE ADICIONADA AQUI

    @GetMapping("/{pedidoId}/propostas")
    @Operation(summary = "Lista as propostas ativas/pendentes recebidas para um pedido (Tela C05)", security = @SecurityRequirement(name = "bearerAuth"))
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
    @Operation(summary = "Aceita uma proposta e atribui o pedido ao profissional escolhido (UC-09)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Aceite realizado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Usuário autenticado não é o dono do pedido"),
            @ApiResponse(responseCode = "404", description = "Pedido ou proposta não encontrados"),
            @ApiResponse(responseCode = "422", description = "Pedido não está com status ABERTO")
    })
    public ResponseEntity<AceitarPropostaResponseDTO> aceitarProposta(
            @PathVariable Long pedidoId,
            @PathVariable Long propostaId,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {

        if (!(usuarioAutenticado instanceof Cliente clienteAutenticado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas clientes podem aceitar propostas.");
        }

        AceitarPropostaResponseDTO response = pedidoService.aceitarProposta(pedidoId, propostaId, clienteAutenticado.getId());
        return ResponseEntity.ok(response);
    }
}