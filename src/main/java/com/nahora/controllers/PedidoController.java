package com.nahora.controllers;

import com.nahora.dto.request.PedidoFiltroRequest;
import com.nahora.dto.request.PedidoRequest;
import com.nahora.dto.response.PedidoResponse;
import com.nahora.dto.response.PedidoResumoResponse;
import com.nahora.model.Cliente;
import com.nahora.model.Pedido;
import com.nahora.model.Usuario;
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
    }
}
