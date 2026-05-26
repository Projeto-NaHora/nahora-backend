package com.nahora.services;

import com.nahora.dto.request.ProfissionalPerfilRequest;
import com.nahora.dto.response.PerfilProfissionalResponseDTO;
import com.nahora.model.Profissional;
import com.nahora.model.enums.CategoriaServico;
import com.nahora.model.enums.StatusVerificacao;
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

    @Mock
    private PushNotificationService pushNotificationService;

    @InjectMocks
    private ProfissionalService profissionalService;

    @Test
    @DisplayName("Deve atualizar o perfil com sucesso e converter as coordenadas para Point")
    void atualizarPerfil_Sucesso() {

        Long profissionalId = 1L;
        ProfissionalPerfilRequest request = new ProfissionalPerfilRequest(
                null, null, null, null, null, null, null,
                5,
                "Sou um ótimo eletricista",
                List.of("Instalação Residencial"),
                List.of(CategoriaServico.ELETRICA),
                15.0,
                -8.047562,
                -34.876964,
                List.of("url1.jpg", "url2.jpg")
        );

        Profissional profissionalMock = new Profissional();
        profissionalMock.setId(profissionalId);
        profissionalMock.setNome("Carlos Silva");
        profissionalMock.setPerfilCompleto(false);
        profissionalMock.setStatusVerificacao(StatusVerificacao.VERIFICADO);

        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.of(profissionalMock));
        when(profissionalRepository.save(any(Profissional.class))).thenAnswer(i -> i.getArguments()[0]);

        PerfilProfissionalResponseDTO response = profissionalService.atualizarPerfil(profissionalId, request);

        assertNotNull(response);
        assertEquals("Sou um ótimo eletricista", response.bio());
        assertEquals(5, response.anosExperiencia());
        assertTrue(profissionalMock.getPerfilCompleto());

        assertNotNull(profissionalMock.getLocalizacao());
        assertEquals(-34.876964, profissionalMock.getLocalizacao().getX());
        assertEquals(-8.047562, profissionalMock.getLocalizacao().getY());

        verify(profissionalRepository).save(profissionalMock);
    }

    @Test
    @DisplayName("Deve lançar 403 quando o profissional não estiver VERIFICADO")
    void atualizarPerfil_StatusInvalido() {

        Long profissionalId = 2L;
        ProfissionalPerfilRequest request = new ProfissionalPerfilRequest(
                null, null, null, null, null, null, null, null, "Bio", null, null, null, null, null, null
        );

        Profissional profissionalMock = new Profissional();
        profissionalMock.setId(profissionalId);
        profissionalMock.setStatusVerificacao(StatusVerificacao.AGUARDANDO_VERIFICACAO);

        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.of(profissionalMock));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> profissionalService.atualizarPerfil(profissionalId, request));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(profissionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção 404 quando o profissional não for encontrado")
    void atualizarPerfil_ProfissionalNaoEncontrado() {

        Long profissionalId = 99L;
        ProfissionalPerfilRequest request = new ProfissionalPerfilRequest(
                null, null, null, null, null, null, null, null, "Bio", null, null, null, null, null, null
        );

        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> profissionalService.atualizarPerfil(profissionalId, request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(profissionalRepository, never()).save(any());
    }
}
