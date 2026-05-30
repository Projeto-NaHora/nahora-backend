package com.nahora.controllers;

import com.nahora.dto.request.ConclusaoRequestDTO;
import com.nahora.dto.response.ConfirmacaoResponseDTO;
import com.nahora.dto.response.PedidoCardDTO;
import com.nahora.dto.request.PedidoFiltroRequest;
import com.nahora.dto.request.PedidoRequest;
import com.nahora.dto.response.PedidoResponse;
import com.nahora.dto.response.PedidoResumoResponse;
import com.nahora.dto.response.PedidoPublicoResponse;
import com.nahora.model.Cliente;
import com.nahora.model.Pedido;
import com.nahora.model.Profissional;
import com.nahora.model.Usuario;
import com.nahora.model.enums.StatusPedido;
import com.nahora.services.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    @PostMapping("/{pedidoId}/concluir")
    @Operation(summary = "Marca pedido como concluído pelo profissional (UC-10)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido marcado como concluído"),
            @ApiResponse(responseCode = "403", description = "Usuário não é o profissional atribuído"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
            @ApiResponse(responseCode = "422", description = "Pedido não está EM_ANDAMENTO")
    })
    public ResponseEntity<ConfirmacaoResponseDTO> concluirPedido(
            @PathVariable Long pedidoId,
            @RequestBody(required = false) ConclusaoRequestDTO dto,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {

        if (!(usuarioAutenticado instanceof Profissional profissional)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas profissionais podem concluir pedidos.");
        }
        return ResponseEntity.ok(pedidoService.marcarComoConcluido(pedidoId, profissional.getId(), dto));
    }

    @PostMapping("/{pedidoId}/confirmar-conclusao")
    @Operation(summary = "Cliente confirma a conclusão do serviço (UC-10)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conclusão confirmada, pagamento liberado"),
            @ApiResponse(responseCode = "403", description = "Usuário não é o cliente do pedido"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
            @ApiResponse(responseCode = "422", description = "Pedido não está AGUARDANDO_VALIDACAO")
    })
    public ResponseEntity<ConfirmacaoResponseDTO> confirmarConclusao(
            @PathVariable Long pedidoId,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {

        if (!(usuarioAutenticado instanceof Cliente cliente)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas clientes podem confirmar a conclusão.");
        }
        return ResponseEntity.ok(pedidoService.confirmarConclusao(pedidoId, cliente.getId(), false));
    }

    @PostMapping("/{pedidoId}/reportar-problema")
    @Operation(summary = "Cliente contesta a conclusão e abre disputa (UC-10)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disputa aberta com sucesso"),
            @ApiResponse(responseCode = "403", description = "Usuário não é o cliente do pedido"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
            @ApiResponse(responseCode = "422", description = "Pedido não está AGUARDANDO_VALIDACAO")
    })
    public ResponseEntity<ConfirmacaoResponseDTO> reportarProblema(
            @PathVariable Long pedidoId,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {

        if (!(usuarioAutenticado instanceof Cliente cliente)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas clientes podem contestar a conclusão.");
        }
        return ResponseEntity.ok(pedidoService.reportarProblema(pedidoId, cliente.getId()));
    }

    @GetMapping("/{pedidoId}/confirmacao")
    @Operation(summary = "Retorna o status de confirmação bilateral do pedido (UC-10)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados de confirmação retornados"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    public ResponseEntity<ConfirmacaoResponseDTO> getConfirmacao(
            @PathVariable Long pedidoId) {
        return ResponseEntity.ok(pedidoService.getConfirmacao(pedidoId));
    }

    @GetMapping
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Lista os pedidos do cliente autenticado formatados para C01",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetro status inválido"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Usuário autenticado não é cliente")
    })
    public ResponseEntity<Page<PedidoCardDTO>> listarPedidosDoCliente(
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {

        if (!(usuarioAutenticado instanceof Cliente clienteAutenticado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas clientes podem acessar seus pedidos.");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "criadoEm"));

        Page<PedidoCardDTO> pedidosPage = pedidoService.listarPedidosDoCliente(clienteAutenticado.getId(), status, pageable);

        return ResponseEntity.ok(pedidosPage);
    }
}