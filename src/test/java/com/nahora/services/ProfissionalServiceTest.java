package com.nahora.services;

import com.nahora.dto.request.ProfissionalPerfilRequest;
import com.nahora.dto.response.AdminProfissionalPendenteDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
                null, null, null, null, null, null,
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
                null, null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, "Bio", null, null, null, null, null, null
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
                null, null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, "Bio", null, null, null, null, null, null
        );

        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> profissionalService.atualizarPerfil(profissionalId, request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(profissionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("listarPendentes — deve retornar página de DTOs mapeados a partir de AGUARDANDO_VERIFICACAO")
    void listarPendentes_Sucesso() {
        Profissional profissional = new Profissional();
        profissional.setId(10L);
        profissional.setNome("Ana Lima");
        profissional.setTelefone("81999990000");
        profissional.setEmail("ana@email.com");
        profissional.setRgFrenteUrl("url-rg-frente");
        profissional.setRgVersoUrl("url-rg-verso");
        profissional.setSelfieUrl("url-selfie");
        profissional.setCriadoEm(LocalDateTime.of(2026, 1, 10, 12, 0));
        profissional.setStatusVerificacao(StatusVerificacao.AGUARDANDO_VERIFICACAO);

        Pageable pageable = PageRequest.of(0, 20);
        Page<Profissional> pageResult = new PageImpl<>(List.of(profissional), pageable, 1);

        when(profissionalRepository.findByStatusVerificacao(eq(StatusVerificacao.AGUARDANDO_VERIFICACAO), eq(pageable)))
                .thenReturn(pageResult);

        Page<AdminProfissionalPendenteDTO> result = profissionalService.listarPendentes(pageable);

        assertEquals(1, result.getTotalElements());
        AdminProfissionalPendenteDTO dto = result.getContent().get(0);
        assertEquals(10L, dto.id());
        assertEquals("Ana Lima", dto.nome());
        assertEquals("81999990000", dto.telefone());
        assertEquals("ana@email.com", dto.email());
        assertEquals("url-rg-frente", dto.rgFrente());
        assertEquals("url-rg-verso", dto.rgVerso());
        assertEquals("url-selfie", dto.selfieComDocumento());
        assertEquals(LocalDateTime.of(2026, 1, 10, 12, 0), dto.criadoEm());

        verify(profissionalRepository).findByStatusVerificacao(StatusVerificacao.AGUARDANDO_VERIFICACAO, pageable);
    }

    @Test
    @DisplayName("listarPendentes — deve retornar página vazia quando não há profissionais aguardando")
    void listarPendentes_Vazio() {
        Pageable pageable = PageRequest.of(0, 20);
        when(profissionalRepository.findByStatusVerificacao(eq(StatusVerificacao.AGUARDANDO_VERIFICACAO), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        Page<AdminProfissionalPendenteDTO> result = profissionalService.listarPendentes(pageable);

        assertTrue(result.isEmpty());
        verify(profissionalRepository).findByStatusVerificacao(StatusVerificacao.AGUARDANDO_VERIFICACAO, pageable);
    }
}
