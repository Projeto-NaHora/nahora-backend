package com.nahora.services;

import com.nahora.dto.request.CategoriaDTO;
import com.nahora.model.Categoria;
import com.nahora.model.enums.CategoriaServico;
import com.nahora.repositories.CategoriaRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    private Categoria categoria(String nome, String icone, CategoriaServico cs) {
        Categoria c = new Categoria();
        c.setNome(nome);
        c.setIcone(icone);
        c.setCategoriaServico(cs);
        return c;
    }

    @Test
    void listarCategorias_DeveRetornarTodasAsCategoriasMapeadas() {
        when(categoriaRepository.findAllByOrderByNomeAsc()).thenReturn(List.of(
                categoria("Elétrica", "⚡", CategoriaServico.ELETRICA),
                categoria("Marcenaria", "🪵", CategoriaServico.MARCENARIA)
        ));

        List<CategoriaDTO> resultado = categoriaService.listarCategorias();

        assertThat(resultado).hasSize(2);
    }

    @Test
    void listarCategorias_DeveMapearCamposCorretamente() {
        when(categoriaRepository.findAllByOrderByNomeAsc()).thenReturn(
                List.of(categoria("Elétrica", "⚡", CategoriaServico.ELETRICA))
        );

        List<CategoriaDTO> resultado = categoriaService.listarCategorias();

        CategoriaDTO dto = resultado.get(0);
        assertThat(dto.getId()).isEqualTo("ELETRICA");
        assertThat(dto.getNome()).isEqualTo("Elétrica");
        assertThat(dto.getIcone()).isEqualTo("⚡");
    }

    @Test
    void listarCategorias_QuandoRepositorioRetornaVazio_DeveRetornarListaVazia() {
        when(categoriaRepository.findAllByOrderByNomeAsc()).thenReturn(List.of());

        assertThat(categoriaService.listarCategorias()).isEmpty();
    }
}
