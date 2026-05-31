package com.nahora.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahora.dto.request.PedidoEditarRequest;
import com.nahora.dto.response.PedidoResponse;
import com.nahora.model.Cliente;
import com.nahora.services.PedidoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PedidoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PedidoService pedidoService;

    private void simularUsuarioAutenticado(Cliente cliente) {
        // forçar o Spring Securit a reconhecer o objeto Cliente real dentro do contexto de autenticação.
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(cliente, null, List.of(() -> "ROLE_CLIENTE"));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("PUT /api/v1/pedidos/{id} deve retornar 200 OK ao editar com sucesso")
    void controllerDeveRetornar200AoEditar() throws Exception {
        Long pedidoId = 1L;
        PedidoEditarRequest request = new PedidoEditarRequest("Nova descricao com tamanho valido de teste", List.of(), null);
        PedidoResponse response = new PedidoResponse();
        response.setId(pedidoId);
        response.setDescricao(request.descricao());

        Cliente mockCliente = new Cliente();
        mockCliente.setId(10L);

        // Força a autenticação do Cliente no contexto integrado do Spring
        simularUsuarioAutenticado(mockCliente);

        when(pedidoService.atualizarPedido(eq(pedidoId), eq(10L), any(PedidoEditarRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/pedidos/" + pedidoId)
                        .with(csrf()) // Evita o bloqueio de CSRF global
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Nova descricao com tamanho valido de teste"));
    }

    @Test
    @DisplayName("PUT /api/v1/pedidos/{id} deve retornar 422 ao tentar editar pedido fechado")
    void controllerDeveRetornar422QuandoStatusInvalido() throws Exception {
        Long pedidoId = 1L;
        PedidoEditarRequest request = new PedidoEditarRequest("Nova descricao com tamanho valido de teste", List.of(), null);

        Cliente mockCliente = new Cliente();
        mockCliente.setId(10L);

        // Força a autenticação do Cliente no contexto integrado do Spring
        simularUsuarioAutenticado(mockCliente);

        when(pedidoService.atualizarPedido(eq(pedidoId), any(), any(PedidoEditarRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Apenas pedidos com status ABERTO podem ser editados"));

        mockMvc.perform(put("/api/v1/pedidos/" + pedidoId)
                        .with(csrf()) // Evita o bloqueio de CSRF global
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }
}