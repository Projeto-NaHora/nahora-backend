package com.nahora.controllers;

import com.nahora.dto.request.CompletarPerfilRequestDTO;
import com.nahora.dto.response.PerfilProfissionalResponseDTO;
import com.nahora.model.Cliente;
import com.nahora.model.Profissional;
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

import java.util.List;

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
    void completarPerfil_AcessoPermitido() {
        
        Profissional profissionalLogado = new Profissional();
        profissionalLogado.setId(1L);

        CompletarPerfilRequestDTO request = new CompletarPerfilRequestDTO(
                "Bio", List.of(), List.of(), 5, 10.0, 0.0, 0.0, List.of()
        );

        PerfilProfissionalResponseDTO responseMock = new PerfilProfissionalResponseDTO(
                1L, "Nome", null, "Bio", null, null, 5, 10.0, 0.0, 0, 0, null, false, null
        );

        when(profissionalService.completarPerfil(1L, request)).thenReturn(responseMock);

        ResponseEntity<PerfilProfissionalResponseDTO> response = profissionalController.completarPerfil(request, profissionalLogado);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Bio", response.getBody().bio());
        verify(profissionalService).completarPerfil(1L, request);
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden quando o usuário logado for um Cliente")
    void completarPerfil_AcessoNegado() {
    
        Cliente clienteLogado = new Cliente();
        clienteLogado.setId(2L);

        CompletarPerfilRequestDTO request = new CompletarPerfilRequestDTO(
                "Bio", List.of(), List.of(), 5, 10.0, 0.0, 0.0, List.of()
        );

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> profissionalController.completarPerfil(request, clienteLogado));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Acesso restrito para profissionais.", exception.getReason());
        
        // Garante que o Service nunca foi chamado
        verify(profissionalService, never()).completarPerfil(anyLong(), any());
    }
}