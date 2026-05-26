package com.nahora.controllers;

import com.nahora.dto.request.ProfissionalPerfilRequest;
import com.nahora.dto.response.PerfilProfissionalResponseDTO;
import com.nahora.model.Cliente;
import com.nahora.model.Profissional;
import com.nahora.model.enums.StatusVerificacao;
import com.nahora.services.ProfissionalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfissionalControllerTest {

    @Mock
    private ProfissionalService profissionalService;

    @InjectMocks
    private ProfissionalController profissionalController;

    @Test
    @DisplayName("Deve retornar 200 OK quando o usuário logado for um Profissional")
    void atualizarPerfil_AcessoPermitido() {

        Profissional profissionalLogado = new Profissional();
        profissionalLogado.setId(1L);
        profissionalLogado.setStatusVerificacao(StatusVerificacao.VERIFICADO);

        ProfissionalPerfilRequest request = new ProfissionalPerfilRequest(
                null, null, null, null, null, null, null, 5, "Bio", null, null, 10.0, null, null, null
        );

        PerfilProfissionalResponseDTO responseMock = new PerfilProfissionalResponseDTO(
                1L, "Nome", null, null, null, "Bio", null, null, 5, 10.0, 0.0, 0, 0, null, false, null
        );

        when(profissionalService.atualizarPerfil(1L, request)).thenReturn(responseMock);

        ResponseEntity<PerfilProfissionalResponseDTO> response = profissionalController.atualizarPerfil(request, profissionalLogado);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Bio", response.getBody().bio());
        verify(profissionalService).atualizarPerfil(1L, request);
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden quando o usuário logado for um Cliente")
    void atualizarPerfil_AcessoNegado() {

        Cliente clienteLogado = new Cliente();
        clienteLogado.setId(2L);

        ProfissionalPerfilRequest request = new ProfissionalPerfilRequest(
                null, null, null, null, null, null, null, 5, "Bio", null, null, 10.0, null, null, null
        );

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> profissionalController.atualizarPerfil(request, clienteLogado));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Acesso restrito para profissionais.", exception.getReason());

        verify(profissionalService, never()).atualizarPerfil(anyLong(), any());
    }
}
