package com.nahora.services;

import com.nahora.dto.request.CategoriaDTO;
import com.nahora.model.enums.CategoriaServico;
import com.nahora.service.CategoriaService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @InjectMocks
    private CategoriaService categoriaService;

    @Test
    void listarCategorias_DeveRetornarTodasAsCategoriasDoEnum() {
        List<CategoriaDTO> resultado = categoriaService.listarCategorias();

        assertThat(resultado).isNotEmpty();
        assertThat(resultado).hasSize(CategoriaServico.values().length);
    }

    @Test
    void listarCategorias_DeveRetornarListaOrdenadaAlfabeticamentePorNome() {
        List<CategoriaDTO> resultado = categoriaService.listarCategorias();


        assertThat(resultado.get(0).getNome()).isEqualTo("Ar Condicionado");
        assertThat(resultado.get(1).getNome()).isEqualTo("Elétrica");
        assertThat(resultado.get(resultado.size() - 1).getNome()).isEqualTo("Pintura");
    }

    @Test
    void listarCategorias_DeveMapearCamposDoEnumCorretamente() {
        List<CategoriaDTO> resultado = categoriaService.listarCategorias();

        CategoriaDTO eletrica = resultado.stream()
                .filter(c -> "ELETRICA".equals(c.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Categoria ELETRICA não encontrada na listagem"));

        assertThat(eletrica.getNome()).isEqualTo("Elétrica");
        assertThat(eletrica.getIcone()).isEqualTo("⚡");
    }
}