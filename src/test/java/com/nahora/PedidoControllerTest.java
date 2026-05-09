package com.nahora;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nahora.controllers.PedidoController;
import com.nahora.dto.request.EnderecoRequest;
import com.nahora.dto.request.PedidoRequest;
import com.nahora.model.Cliente;
import com.nahora.model.Endereco;
import com.nahora.model.Pedido;
import com.nahora.model.enums.CategoriaServico;
import com.nahora.model.enums.StatusPedido;
import com.nahora.model.enums.Urgencia;
import com.nahora.services.PedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PedidoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PedidoService pedidoService;

    @InjectMocks
    private PedidoController pedidoController;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pedidoController).build();
    }


    @Test
    void criarPedido_ComDadosValidos_DeveRetornar201() throws Exception {
        PedidoRequest request = new PedidoRequest();
        request.setClienteId(1L);
        request.setCategoria(CategoriaServico.ELETRICA);
        request.setDescricao("Troca de tomada");
        request.setUrgencia(Urgencia.NORMAL);
        request.setOrcamentoEstimado(BigDecimal.valueOf(150));
        request.setDataDesejada(LocalDateTime.now().plusDays(5));
        request.setFotos(List.of("foto1"));

        Cliente clienteMock = new Cliente();
        clienteMock.setId(1L);
        clienteMock.setNome("João");

        Endereco enderecoMock = new Endereco();
        enderecoMock.setLogradouro("Rua Teste");

        Pedido pedidoMock = new Pedido();
        pedidoMock.setId(100L);
        pedidoMock.setStatus(StatusPedido.ABERTO);
        pedidoMock.setCliente(clienteMock);
        pedidoMock.setEndereco(enderecoMock);

        when(pedidoService.criarPedido(any(PedidoRequest.class))).thenReturn(pedidoMock);

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("ABERTO"));
    }

    @Test
    void criarPedido_ComDataDesejadaPassada_DeveRetornar400() throws Exception {
        PedidoRequest request = new PedidoRequest();
        request.setClienteId(1L);
        request.setCategoria(CategoriaServico.ELETRICA);
        request.setDescricao("Teste");
        request.setUrgencia(Urgencia.NORMAL);
        request.setDataDesejada(LocalDateTime.now().minusDays(1));

        EnderecoRequest endereco = new EnderecoRequest();
        endereco.setLogradouro("Rua X");
        endereco.setNumero("1");
        endereco.setBairro("Centro");
        endereco.setCidade("Cidade");
        endereco.setEstado("SP");
        endereco.setCep("00000-000");
        request.setEndereco(endereco);

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void criarPedido_ComMaisDe5Fotos_DeveRetornar400() throws Exception {
        PedidoRequest request = new PedidoRequest();
        request.setClienteId(1L);
        request.setCategoria(CategoriaServico.ELETRICA);
        request.setDescricao("Teste");
        request.setUrgencia(Urgencia.NORMAL);
        request.setDataDesejada(LocalDateTime.now().plusDays(1));
        request.setFotos(List.of("foto1", "foto2", "foto3", "foto4", "foto5", "foto6"));

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void criarPedido_ComClienteInexistente_DeveRetornar404() throws Exception {
        PedidoRequest request = new PedidoRequest();
        request.setClienteId(999L);
        request.setCategoria(CategoriaServico.ELETRICA);
        request.setDescricao("Teste");
        request.setUrgencia(Urgencia.NORMAL);
        request.setDataDesejada(LocalDateTime.now().plusDays(1));

        when(pedidoService.criarPedido(any(PedidoRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado"));

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void criarPedido_ComLimiteDePedidosExcedido_DeveRetornar409() throws Exception {
        PedidoRequest request = new PedidoRequest();
        request.setClienteId(1L);
        request.setCategoria(CategoriaServico.ELETRICA);
        request.setDescricao("Teste");
        request.setUrgencia(Urgencia.NORMAL);
        request.setDataDesejada(LocalDateTime.now().plusDays(1));

        when(pedidoService.criarPedido(any(PedidoRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Cliente possui 3 pedidos em aberto"));

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
