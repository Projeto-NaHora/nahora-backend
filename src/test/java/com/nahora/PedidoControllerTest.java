package com.nahora;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nahora.controllers.PedidoController;
import com.nahora.dto.response.PedidoCardDTO;
import com.nahora.dto.request.EnderecoRequest;
import com.nahora.dto.request.PedidoFiltroRequest;
import com.nahora.dto.request.PedidoRequest;
import com.nahora.dto.response.AceitarPropostaResponseDTO;
import com.nahora.dto.response.PedidoResponse;
import com.nahora.dto.response.PedidoResumoResponse;
import com.nahora.dto.response.PropostaResponseDTO;
import com.nahora.model.Cliente;
import com.nahora.model.Endereco;
import com.nahora.model.Pedido;
import com.nahora.model.enums.CategoriaServico;
import com.nahora.model.enums.StatusPedido;
import com.nahora.model.enums.StatusProposta;
import com.nahora.model.enums.Urgencia;
import com.nahora.services.PedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.nullValue;

@Tag("unit")
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
        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                Cliente cliente = new Cliente();
                cliente.setId(1L);
                cliente.setNome("João");
                return cliente;
            }
        };

        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(pedidoController)
                .setCustomArgumentResolvers(authPrincipalResolver, new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(converter)
                .build();
    }

    @Test
    void criarPedido_ComDadosValidos_DeveRetornar201() throws Exception {
        PedidoRequest request = new PedidoRequest();
        request.setCategoria(CategoriaServico.ELETRICA);
        request.setDescricao("Troca de tomada da parede de casa");
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

        when(pedidoService.criarPedido(eq(1L), any(PedidoRequest.class))).thenReturn(pedidoMock);

        PedidoResponse responseMock = new PedidoResponse();
        responseMock.setId(100L);
        responseMock.setStatus(StatusPedido.ABERTO);
        when(pedidoService.toResponseDTO(any(Pedido.class))).thenReturn(responseMock);

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
        request.setCategoria(CategoriaServico.ELETRICA);
        request.setDescricao("Uma descrição super longa");
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
        request.setCategoria(CategoriaServico.ELETRICA);
        request.setDescricao("Uma descrição super longa");
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
        request.setCategoria(CategoriaServico.ELETRICA);
        request.setDescricao("Uma descrição super longa");
        request.setUrgencia(Urgencia.NORMAL);
        request.setDataDesejada(LocalDateTime.now().plusDays(1));

        when(pedidoService.criarPedido(eq(1L), any(PedidoRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado"));

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void criarPedido_ComDescricaoMenorQue20Caracteres_DeveRetornar400() throws Exception {
        PedidoRequest request = new PedidoRequest();
        request.setCategoria(CategoriaServico.ELETRICA);
        request.setDescricao("curta demais");
        request.setUrgencia(Urgencia.NORMAL);
        request.setDataDesejada(LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void criarPedido_ComLimiteDePedidosExcedido_DeveRetornar409() throws Exception {
        PedidoRequest request = new PedidoRequest();
        request.setCategoria(CategoriaServico.ELETRICA);
        request.setDescricao("Uma descrição super longa");
        request.setUrgencia(Urgencia.NORMAL);
        request.setDataDesejada(LocalDateTime.now().plusDays(1));

        when(pedidoService.criarPedido(eq(1L), any(PedidoRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Cliente possui 3 pedidos em aberto"));

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void listarPedidosParaProfissional_DeveRetornar200_ComListaDePedidos() throws Exception {
        PedidoResumoResponse resumo = PedidoResumoResponse.builder()
                .id(1L)
                .descricao("Conserto de chuveiro...")
                .categoria(CategoriaServico.ELETRICA)
                .distanciaKm(5.5)
                .urgente(true)
                .contadorPropostas(2)
                .dataPublicacao(LocalDateTime.now())
                .build();

        Page<PedidoResumoResponse> pageMock = new PageImpl<>(List.of(resumo), PageRequest.of(0, 10), 1);

        when(pedidoService.listarPedidosComFiltros(any(PedidoFiltroRequest.class), any(Pageable.class)))
                .thenReturn(pageMock);

        mockMvc.perform(get("/api/v1/pedidos/disponiveis")
                        .param("categoria", "ELETRICA")
                        .param("urgente", "true")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].categoria").value("ELETRICA"))
                .andExpect(jsonPath("$.content[0].distanciaKm").value(5.5))
                .andExpect(jsonPath("$.content[0].urgente").value(true));
    }

    @Test
    void listarPedidosParaProfissional_QuandoPaginaVazia_DeveRetornar200() throws Exception {
        Page<PedidoResumoResponse> pageMock = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(pedidoService.listarPedidosComFiltros(any(PedidoFiltroRequest.class), any(Pageable.class)))
                .thenReturn(pageMock);

        mockMvc.perform(get("/api/v1/pedidos/disponiveis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void listarPedidosParaProfissional_QuandoProfissionalSemLocalizacao_DeveRetornar400() throws Exception {
        when(pedidoService.listarPedidosComFiltros(any(PedidoFiltroRequest.class), any(Pageable.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profissional não possui localização"));

        mockMvc.perform(get("/api/v1/pedidos/disponiveis"))
                .andExpect(status().isBadRequest());
    } // <--- CHAVE DE FECHAMENTO ADICIONADA AQUI

    @Test
    void listarPropostas_ComoCliente_DeveRetornar200ComLista() throws Exception {
        PropostaResponseDTO dto = new PropostaResponseDTO(
                1L, "Carlos Silva", null, 4.8, 30, 15, 2.5,
                "Faço rápido e bem feito", BigDecimal.valueOf(200.00), List.of(), StatusProposta.ATIVA
        );

        when(pedidoService.listarPropostasAtivas(eq(10L), eq(1L), isNull()))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/pedidos/10/propostas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].profissionalNome").value("Carlos Silva"))
                .andExpect(jsonPath("$[0].valorProposto").value(200.00));
    }

    @Test
    void listarPropostas_PedidoNaoEncontrado_DeveRetornar404() throws Exception {
        when(pedidoService.listarPropostasAtivas(eq(99L), eq(1L), isNull()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));

        mockMvc.perform(get("/api/v1/pedidos/99/propostas"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarPropostas_UsuarioNaoDonoDoPedido_DeveRetornar403() throws Exception {
        when(pedidoService.listarPropostasAtivas(eq(10L), eq(1L), isNull()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário não autorizado"));

        mockMvc.perform(get("/api/v1/pedidos/10/propostas"))
                .andExpect(status().isForbidden());
    }

    @Test
    void aceitarProposta_ComDadosValidos_DeveRetornar200() throws Exception {
        AceitarPropostaResponseDTO resposta = new AceitarPropostaResponseDTO(
                10L, StatusPedido.EM_ANDAMENTO, 100L, "Carlos Silva"
        );

        when(pedidoService.aceitarProposta(eq(10L), eq(50L), eq(1L))).thenReturn(resposta);

        mockMvc.perform(post("/api/v1/pedidos/10/propostas/50/aceitar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidoId").value(10))
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.profissionalNome").value("Carlos Silva"));
    }

    @Test
    void aceitarProposta_PedidoNaoAberto_DeveRetornar422() throws Exception {
        when(pedidoService.aceitarProposta(eq(10L), eq(50L), eq(1L)))
                .thenThrow(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Pedido não está com status ABERTO"));

        mockMvc.perform(post("/api/v1/pedidos/10/propostas/50/aceitar"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void aceitarProposta_UsuarioNaoDonoDoPedido_DeveRetornar403() throws Exception {
        when(pedidoService.aceitarProposta(eq(10L), eq(50L), eq(1L)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário autenticado não é o dono do pedido"));

        mockMvc.perform(post("/api/v1/pedidos/10/propostas/50/aceitar"))
                .andExpect(status().isForbidden());
    }

    @Test
    void aceitarProposta_PropostaOuPedidoNaoEncontrado_DeveRetornar404() throws Exception {
        when(pedidoService.aceitarProposta(eq(10L), eq(99L), eq(1L)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposta não encontrada"));

        mockMvc.perform(post("/api/v1/pedidos/10/propostas/99/aceitar"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarMeusPedidos_SemFiltroStatus_DeveRetornar200ComPedidos() throws Exception {
        PedidoResponse r1 = new PedidoResponse();
        r1.setId(1L);
        r1.setStatus(StatusPedido.ABERTO);

        PedidoResponse r2 = new PedidoResponse();
        r2.setId(2L);
        r2.setStatus(StatusPedido.EM_ANDAMENTO);

        Page<PedidoResponse> pageMock = new PageImpl<>(List.of(r1, r2), PageRequest.of(0, 10), 2);

        when(pedidoService.listarMeusPedidos(eq(1L), isNull(), any(Pageable.class)))
                .thenReturn(pageMock);

        mockMvc.perform(get("/api/v1/pedidos/meus")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void listarMeusPedidos_ComFiltroStatus_DeveRetornar200ComPedidosFiltrados() throws Exception {
        PedidoResponse r = new PedidoResponse();
        r.setId(3L);
        r.setStatus(StatusPedido.ABERTO);

        Page<PedidoResponse> pageMock = new PageImpl<>(List.of(r), PageRequest.of(0, 10), 1);

        when(pedidoService.listarMeusPedidos(eq(1L), eq(List.of(StatusPedido.ABERTO)), any(Pageable.class)))
                .thenReturn(pageMock);

        mockMvc.perform(get("/api/v1/pedidos/meus")
                        .param("status", "ABERTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(3))
                .andExpect(jsonPath("$.content[0].status").value("ABERTO"));
    }

    @Test
    void listarMeusPedidos_QuandoNaoHaPedidos_DeveRetornar200ComListaVazia() throws Exception {
        Page<PedidoResponse> pageMock = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(pedidoService.listarMeusPedidos(eq(1L), isNull(), any(Pageable.class)))
                .thenReturn(pageMock);

        mockMvc.perform(get("/api/v1/pedidos/meus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listarPedidosDoCliente_SemFiltroStatus_DeveRetornar200ComCards() throws Exception {
        PedidoCardDTO card1 = PedidoCardDTO.builder()
                .id(100L)
                .titulo("Elétrica")
                .status(StatusPedido.ABERTO)
                .data("20/05/2026")
                .descricao("Conserto do chuveiro fiação")
                .endereco("Rua X, 10 - Centro, Recife")
                .build();

        Page<PedidoCardDTO> pageMock = new PageImpl<>(List.of(card1), PageRequest.of(0, 10), 1);

        when(pedidoService.listarPedidosDoCliente(eq(1L), isNull(), any(Pageable.class)))
                .thenReturn(pageMock);

        mockMvc.perform(get("/api/v1/pedidos")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.content[0].titulo").value("Elétrica"))
                .andExpect(jsonPath("$.content[0].status").value("ABERTO"))
                .andExpect(jsonPath("$.content[0].data").value("20/05/2026"))
                .andExpect(jsonPath("$.content[0].endereco").value("Rua X, 10 - Centro, Recife"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listarPedidosDoCliente_ComFiltroStatus_DeveRetornar200ComCardsFiltrados() throws Exception {
        PedidoCardDTO card2 = PedidoCardDTO.builder()
                .id(101L)
                .titulo("Pintura")
                .status(StatusPedido.EM_ANDAMENTO)
                .data("18/05/2026")
                .descricao("Pintura de salas")
                .profissionalNome("Fernando Pintor")
                .build();

        Page<PedidoCardDTO> pageMock = new PageImpl<>(List.of(card2), PageRequest.of(0, 10), 1);

        when(pedidoService.listarPedidosDoCliente(eq(1L), eq(StatusPedido.EM_ANDAMENTO), any(Pageable.class)))
                .thenReturn(pageMock);

        mockMvc.perform(get("/api/v1/pedidos")
                        .param("status", "EM_ANDAMENTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(101))
                .andExpect(jsonPath("$.content[0].status").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.content[0].profissionalNome").value("Fernando Pintor"))
                .andExpect(jsonPath("$.content[0].endereco").value(nullValue()));
    }
}