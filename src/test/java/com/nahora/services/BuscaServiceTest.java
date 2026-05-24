package com.nahora.services;

import com.nahora.dto.response.*;
import com.nahora.model.Categoria;
import com.nahora.model.Profissional;
import com.nahora.model.enums.CategoriaServico;
import com.nahora.repositories.CategoriaRepository;
import com.nahora.repositories.ProfissionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class BuscaServiceTest {

    @Mock private CategoriaRepository categoriaRepository;
    @Mock private ProfissionalRepository profissionalRepository;

    @InjectMocks
    private BuscaService buscaService;

    private Categoria categoriaEletrica;
    private Profissional profissional;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        categoriaEletrica = new Categoria();
        categoriaEletrica.setId(1L);
        categoriaEletrica.setNome("Elétrica");
        categoriaEletrica.setIcone("eletrica");
        categoriaEletrica.setCategoriaServico(CategoriaServico.ELETRICA);
        categoriaEletrica.setValorSugeridoMin(new BigDecimal("80.00"));
        categoriaEletrica.setValorSugeridoMax(new BigDecimal("350.00"));

        profissional = new Profissional();
        profissional.setId(10L);
        profissional.setNome("João Eletricista");
        profissional.setFoto("foto.jpg");
        profissional.setPlanoPlus(false);
        profissional.setDisponivel(true);
        profissional.setAtivo(true);
        profissional.setCategoriasAtendidas(List.of(CategoriaServico.ELETRICA));
        profissional.setCidade("Recife");
        profissional.setEstado("PE");
        profissional.setNotaMedia(4.8);
        profissional.setTotalAvaliacoes(20);
        profissional.setAnosExperiencia(5);
        profissional.setTotalServicosExecutados(42);
        profissional.setEspecialidades(List.of("Instalação", "Reparos"));
        profissional.setBio("Eletricista experiente.");
        profissional.setDescricaoEspecialidades("Especialista em instalações residenciais e comerciais.");
        profissional.setPortfolio(List.of("foto1.jpg", "foto2.jpg", "foto3.jpg", "foto4.jpg"));
        profissional.setPerfilCompleto(true);

        pageable = PageRequest.of(0, 10);

        when(categoriaRepository.findAll()).thenReturn(List.of(categoriaEletrica));
        buscaService.initCategoriaCache();
    }

    @Test
    @DisplayName("listarCategorias deve retornar lista ordenada")
    void listarCategorias_retornaListaOrdenada() {
        when(categoriaRepository.findAllByOrderByNomeAsc()).thenReturn(List.of(categoriaEletrica));

        List<CategoriaDTO> result = buscaService.listarCategorias();

        assertEquals(1, result.size());
        assertEquals("Elétrica", result.get(0).nome());
        assertEquals("eletrica", result.get(0).icone());
        verify(categoriaRepository).findAllByOrderByNomeAsc();
    }

    @Test
    @DisplayName("buscarPorCategoria deve retornar profissionais com totalEncontrados")
    void buscarPorCategoria_sucesso() {
        Page<Profissional> page = new PageImpl<>(List.of(profissional), pageable, 1);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaEletrica));
        when(profissionalRepository.findByCategoria(CategoriaServico.ELETRICA, pageable)).thenReturn(page);

        ProfissionalBuscaResponse response = buscaService.buscarPorCategoria(1L, pageable);

        assertEquals(1, response.totalEncontrados());
        assertEquals(0, response.paginaAtual());
        assertEquals(1, response.profissionais().size());

        ProfissionalCardDTO card = response.profissionais().get(0);
        assertEquals("João Eletricista", card.nome());
        assertEquals("Elétrica", card.categoriaNome());
        assertEquals("Recife", card.cidade());
        assertNull(card.distanciaKm());
    }

    @Test
    @DisplayName("buscarPorCategoria deve lançar 404 quando categoria não existe")
    void buscarPorCategoria_categoriaNaoEncontrada() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> buscaService.buscarPorCategoria(99L, pageable));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(profissionalRepository, never()).findByCategoria(any(), any());
    }

    @Test
    @DisplayName("buscarPorTermo deve retornar profissionais paginados")
    void buscarPorTermo_sucesso() {
        Page<Profissional> page = new PageImpl<>(List.of(profissional), pageable, 1);
        when(profissionalRepository.findByTermoOrdenados("João", pageable)).thenReturn(page);

        ProfissionalBuscaResponse response = buscaService.buscarPorTermo("João", pageable);

        assertEquals(1, response.totalEncontrados());
        assertEquals("João Eletricista", response.profissionais().get(0).nome());
    }

    @Test
    @DisplayName("buscarPorTermo deve lançar 400 quando termo tem menos de 2 caracteres")
    void buscarPorTermo_termoCurto_lancaBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> buscaService.buscarPorTermo("a", pageable));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(profissionalRepository, never()).findByTermoOrdenados(any(), any());
    }

    @Test
    @DisplayName("buscarPorTermo deve lançar 400 quando termo é nulo")
    void buscarPorTermo_termoNulo_lancaBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> buscaService.buscarPorTermo(null, pageable));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("buscarSugeridos deve retornar profissionais com distância calculada")
    void buscarSugeridos_sucesso() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{10L, 2.5});
        when(profissionalRepository.findSugeridosWithDistance(-8.05, -34.88, 10_000.0))
                .thenReturn(rows);
        when(profissionalRepository.findAllById(List.of(10L))).thenReturn(List.of(profissional));

        List<ProfissionalCardDTO> result = buscaService.buscarSugeridos(-8.05, -34.88);

        assertEquals(1, result.size());
        assertEquals(2.5, result.get(0).distanciaKm());
        assertEquals("João Eletricista", result.get(0).nome());
    }

    @Test
    @DisplayName("buscarSugeridos deve retornar lista vazia quando não há profissionais no raio")
    void buscarSugeridos_semResultados() {
        when(profissionalRepository.findSugeridosWithDistance(any(), any(), anyDouble()))
                .thenReturn(List.of());
        when(profissionalRepository.findAllById(List.of())).thenReturn(List.of());

        List<ProfissionalCardDTO> result = buscaService.buscarSugeridos(-8.05, -34.88);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("buscarPerfil deve retornar DTO completo com 3 primeiras fotos do portfólio")
    void buscarPerfil_sucesso() {
        when(profissionalRepository.findById(10L)).thenReturn(Optional.of(profissional));

        ProfissionalPerfilDTO dto = buscaService.buscarPerfil(10L);

        assertEquals(10L, dto.id());
        assertEquals("João Eletricista", dto.nome());
        assertEquals("Elétrica", dto.categoriaNome());
        assertEquals("Recife", dto.cidade());
        assertEquals(4.8, dto.mediaAvaliacoes());
        assertEquals(20, dto.totalAvaliacoes());
        assertEquals(5, dto.anosExperiencia());
        assertEquals(42, dto.totalServicos());
        assertEquals("Eletricista experiente.", dto.sobreDescricao());
        assertEquals(List.of("Instalação", "Reparos"), dto.especialidadesTags());
        assertEquals("Especialista em instalações residenciais e comerciais.", dto.especialidadesDescricao());
        assertEquals(3, dto.portfolioFotos().size());
        assertEquals(4, dto.totalPortfolioFotos());
    }

    @Test
    @DisplayName("buscarPerfil deve lançar 404 quando profissional não existe")
    void buscarPerfil_naoEncontrado() {
        when(profissionalRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> buscaService.buscarPerfil(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("buscarPerfil deve lançar 404 quando profissional está inativo")
    void buscarPerfil_profissionalInativo() {
        profissional.setAtivo(false);
        when(profissionalRepository.findById(10L)).thenReturn(Optional.of(profissional));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> buscaService.buscarPerfil(10L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("listarPortfolio deve retornar página correta")
    void listarPortfolio_retornaPaginaCorreta() {
        when(profissionalRepository.findById(10L)).thenReturn(Optional.of(profissional));

        Pageable page1 = PageRequest.of(0, 2);
        List<String> result = buscaService.listarPortfolio(10L, page1);

        assertEquals(2, result.size());
        assertEquals("foto1.jpg", result.get(0));
        assertEquals("foto2.jpg", result.get(1));
    }

    @Test
    @DisplayName("listarPortfolio deve retornar lista vazia quando página excede total")
    void listarPortfolio_paginaAlemDoTotal() {
        when(profissionalRepository.findById(10L)).thenReturn(Optional.of(profissional));

        Pageable page = PageRequest.of(10, 5);
        List<String> result = buscaService.listarPortfolio(10L, page);

        assertTrue(result.isEmpty());
    }
}
