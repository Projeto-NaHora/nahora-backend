package com.nahora.services;

import com.nahora.dto.request.CompletarPerfilRequestDTO;
import com.nahora.dto.response.PerfilProfissionalResponseDTO;
import com.nahora.model.Profissional;
import com.nahora.model.enums.CategoriaServico;
import com.nahora.repositories.ProfissionalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfissionalServiceTest {

    @Mock
    private ProfissionalRepository profissionalRepository;

    @InjectMocks
    private ProfissionalService profissionalService;

    @Test
    @DisplayName("Deve completar o perfil com sucesso e converter as coordenadas para Point")
    void completarPerfil_Sucesso() {
        
        Long profissionalId = 1L;
        CompletarPerfilRequestDTO request = new CompletarPerfilRequestDTO(
                "Sou um ótimo eletricista",
                List.of(CategoriaServico.ELETRICA), 
                List.of("Instalação Residencial"),
                5,
                15.0,
                -8.047562, // Latitude (Recife)
                -34.876964, // Longitude (Recife)
                List.of("url1.jpg", "url2.jpg")
        );

        Profissional profissionalMock = new Profissional();
        profissionalMock.setId(profissionalId);
        profissionalMock.setNome("Carlos Silva");
        profissionalMock.setPerfilCompleto(false);

        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.of(profissionalMock));
        
        when(profissionalRepository.save(any(Profissional.class))).thenAnswer(i -> i.getArguments()[0]);
        
        PerfilProfissionalResponseDTO response = profissionalService.completarPerfil(profissionalId, request);

        assertNotNull(response);
        assertEquals("Sou um ótimo eletricista", response.bio());
        assertEquals(5, response.anosExperiencia());
        assertTrue(profissionalMock.getPerfilCompleto()); // Verifica se a flag foi alterada
        
        assertNotNull(profissionalMock.getLocalizacao());
        assertEquals(-34.876964, profissionalMock.getLocalizacao().getX()); // X = Longitude
        assertEquals(-8.047562, profissionalMock.getLocalizacao().getY());  // Y = Latitude

        verify(profissionalRepository).save(profissionalMock);
    }

    @Test
    @DisplayName("Deve lançar exceção 404 quando o profissional não for encontrado")
    void completarPerfil_ProfissionalNaoEncontrado() {
      
        Long profissionalId = 99L;
        CompletarPerfilRequestDTO request = new CompletarPerfilRequestDTO(
                "Bio", List.of(), List.of(), 1, 10.0, 0.0, 0.0, List.of()
        );

        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> profissionalService.completarPerfil(profissionalId, request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(profissionalRepository, never()).save(any());
    }
}