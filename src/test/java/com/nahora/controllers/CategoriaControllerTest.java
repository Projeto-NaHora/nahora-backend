package com.nahora.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahora.controllers.CategoriaController;
import com.nahora.dto.request.CategoriaDTO;
import com.nahora.services.CategoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class CategoriaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CategoriaService categoriaService;

    @InjectMocks
    private CategoriaController categoriaController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(categoriaController)
                .setMessageConverters(converter)
                .build();
    }

    @Test
    void listarCategorias_DeveRetornar200EListaDeCategoriasOrdenada() throws Exception {
        CategoriaDTO c1 = CategoriaDTO.builder()
                .id("MARCENARIA")
                .nome("Marcenaria")
                .icone("🪵")
                .build();

        CategoriaDTO c2 = CategoriaDTO.builder()
                .id("ELETRICA")
                .nome("Elétrica")
                .icone("⚡")
                .build();

        List<CategoriaDTO> listaMock = List.of(c1, c2);

        when(categoriaService.listarCategorias()).thenReturn(listaMock);

        mockMvc.perform(get("/api/v1/categorias")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("MARCENARIA"))
                .andExpect(jsonPath("$[0].nome").value("Marcenaria"))
                .andExpect(jsonPath("$[0].icone").value("🪵"))
                .andExpect(jsonPath("$[1].id").value("ELETRICA"))
                .andExpect(jsonPath("$[1].nome").value("Elétrica"))
                .andExpect(jsonPath("$[1].icone").value("⚡"));
    }

    @Test
    void listarCategorias_QuandoNaoHouverCategorias_DeveRetornar200EListaVazia() throws Exception {
        when(categoriaService.listarCategorias()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/categorias")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}