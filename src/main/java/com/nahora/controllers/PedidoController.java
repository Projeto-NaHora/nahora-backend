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
}
