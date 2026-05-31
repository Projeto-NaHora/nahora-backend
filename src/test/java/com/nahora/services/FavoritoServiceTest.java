package com.nahora.services;

import com.nahora.dto.response.FavoritoResponseDTO;
import com.nahora.model.Categoria;
import com.nahora.model.Cliente;
import com.nahora.model.Favorito;
import com.nahora.model.Profissional;
import com.nahora.model.enums.CategoriaServico;
import com.nahora.repositories.CategoriaRepository;
import com.nahora.repositories.ClienteRepository;
import com.nahora.repositories.FavoritoRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class FavoritoServiceTest {

    @Mock private FavoritoRepository favoritoRepository;
    @Mock private ProfissionalRepository profissionalRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private CategoriaRepository categoriaRepository;

    @InjectMocks
    private FavoritoService favoritoService;

    private Cliente cliente;
    private Profissional profissional;
    private Favorito favorito;
    private Categoria categoria;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);

        profissional = new Profissional();
        profissional.setId(10L);
        profissional.setNome("João Eletricista");
        profissional.setFoto("foto.jpg");
        profissional.setNotaMedia(4.8);
        profissional.setTotalAvaliacoes(20);
        profissional.setCategoriasAtendidas(List.of(CategoriaServico.ELETRICA));

        favorito = new Favorito();
        favorito.setId(100L);
        favorito.setCliente(cliente);
        favorito.setProfissional(profissional);
        favorito.setCriadoEm(LocalDateTime.now());

        categoria = new Categoria();
        categoria.setId(2L);
        categoria.setNome("Elétrica");
        categoria.setCategoriaServico(CategoriaServico.ELETRICA);

        pageable = PageRequest.of(0, 10);

        when(categoriaRepository.findAll()).thenReturn(List.of(categoria));
        favoritoService.initCategoriaCache();
    }

    // --- adicionarFavorito ---

    @Test
    @DisplayName("adicionarFavorito deve salvar favorito com sucesso")
    void adicionarFavorito_Sucesso() {
        when(profissionalRepository.existsById(10L)).thenReturn(true);
        when(favoritoRepository.existsByClienteIdAndProfissionalId(1L, 10L)).thenReturn(false);
        when(clienteRepository.getReferenceById(1L)).thenReturn(cliente);
        when(profissionalRepository.getReferenceById(10L)).thenReturn(profissional);

        favoritoService.adicionarFavorito(1L, 10L);

        verify(favoritoRepository).save(any(Favorito.class));
    }

    @Test
    @DisplayName("adicionarFavorito deve lançar 404 quando profissional não existe")
    void adicionarFavorito_ProfissionalNaoEncontrado() {
        when(profissionalRepository.existsById(99L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> favoritoService.adicionarFavorito(1L, 99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(favoritoRepository, never()).save(any());
    }

    @Test
    @DisplayName("adicionarFavorito deve lançar 409 quando favorito já existe")
    void adicionarFavorito_JaFavoritado() {
        when(profissionalRepository.existsById(10L)).thenReturn(true);
        when(favoritoRepository.existsByClienteIdAndProfissionalId(1L, 10L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> favoritoService.adicionarFavorito(1L, 10L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(favoritoRepository, never()).save(any());
    }

    // --- removerFavorito ---

    @Test
    @DisplayName("removerFavorito deve deletar favorito com sucesso")
    void removerFavorito_Sucesso() {
        when(favoritoRepository.findByClienteIdAndProfissionalId(1L, 10L)).thenReturn(Optional.of(favorito));

        favoritoService.removerFavorito(1L, 10L);

        verify(favoritoRepository).delete(favorito);
    }

    @Test
    @DisplayName("removerFavorito deve lançar 404 quando favorito não existe")
    void removerFavorito_NaoEncontrado() {
        when(favoritoRepository.findByClienteIdAndProfissionalId(1L, 99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> favoritoService.removerFavorito(1L, 99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(favoritoRepository, never()).delete(any());
    }

    // --- listarFavoritos ---

    @Test
    @DisplayName("listarFavoritos sem filtro deve retornar página de favoritos")
    void listarFavoritos_SemFiltro() {
        Page<Favorito> page = new PageImpl<>(List.of(favorito), pageable, 1);
        when(favoritoRepository.findByClienteId(eq(1L), any(Pageable.class))).thenReturn(page);

        Page<FavoritoResponseDTO> resultado = favoritoService.listarFavoritos(1L, null, pageable);

        assertEquals(1, resultado.getTotalElements());
        FavoritoResponseDTO dto = resultado.getContent().get(0);
        assertEquals(10L, dto.profissionalId());
        assertEquals("João Eletricista", dto.nomeProfissional());
        assertEquals(4.8, dto.mediaAvaliacao());
        assertEquals(List.of("Elétrica"), dto.categorias());
    }

    @Test
    @DisplayName("listarFavoritos com filtro por categoria deve retornar favoritos filtrados")
    void listarFavoritos_ComFiltroCategoria() {
        Page<Favorito> page = new PageImpl<>(List.of(favorito), pageable, 1);
        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(categoria));
        when(favoritoRepository.findByClienteIdAndCategoria(eq(1L), eq(CategoriaServico.ELETRICA), any(Pageable.class)))
                .thenReturn(page);

        Page<FavoritoResponseDTO> resultado = favoritoService.listarFavoritos(1L, 2L, pageable);

        assertEquals(1, resultado.getTotalElements());
        assertEquals("João Eletricista", resultado.getContent().get(0).nomeProfissional());
    }

    @Test
    @DisplayName("listarFavoritos deve lançar 404 quando categoriaId não existe")
    void listarFavoritos_CategoriaNaoEncontrada() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> favoritoService.listarFavoritos(1L, 99L, pageable));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // --- isFavoritado ---

    @Test
    @DisplayName("isFavoritado deve retornar true quando profissional está favoritado")
    void isFavoritado_RetornaTrue() {
        when(favoritoRepository.existsByClienteIdAndProfissionalId(1L, 10L)).thenReturn(true);

        assertTrue(favoritoService.isFavoritado(1L, 10L));
    }

    @Test
    @DisplayName("isFavoritado deve retornar false quando profissional não está favoritado")
    void isFavoritado_RetornaFalse() {
        when(favoritoRepository.existsByClienteIdAndProfissionalId(1L, 10L)).thenReturn(false);

        assertFalse(favoritoService.isFavoritado(1L, 10L));
    }
}
