package com.nahora.services;

import com.nahora.dto.request.*;
import com.nahora.dto.request.ConclusaoRequestDTO;
import com.nahora.dto.response.ConfirmacaoResponseDTO;
import com.nahora.dto.response.PedidoCardDTO;
import com.nahora.dto.response.PedidoResponse;
import com.nahora.dto.response.PedidoResumoResponse;
import com.nahora.model.*;
import com.nahora.model.enums.CategoriaServico;
import com.nahora.model.enums.StatusPedido;
import com.nahora.model.enums.StatusProposta;
import com.nahora.model.enums.Urgencia;
import com.nahora.repositories.ClienteRepository;
import com.nahora.repositories.PedidoRepository;
import com.nahora.repositories.ProfissionalRepository;
import com.nahora.repositories.PropostaRepository;
import com.nahora.services.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ProfissionalRepository profissionalRepository;

    @Mock
    private PropostaRepository propostaRepository;

    @Mock
    private PushNotificationService pushNotificationService;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private PedidoService pedidoService;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private Cliente cliente;
    private PedidoRequest request;
    private Endereco enderecoSalvo;
    private final Long clienteId = 1L;
    private final Long pedidoId = 1L;
    private final Long pedidoId2 = 2L;

    private void setupSecurityContextMock(MockedStatic<SecurityContextHolder> securityHolder, String email) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(email);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(email);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        securityHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
    }

    @BeforeEach
    void setUp() {
        enderecoSalvo = new Endereco();
        enderecoSalvo.setLogradouro("Rua Salva");
        enderecoSalvo.setNumero("10");
        enderecoSalvo.setBairro("Centro");
        enderecoSalvo.setCidade("São Paulo");
        enderecoSalvo.setEstado("SP");
        enderecoSalvo.setCep("01000-000");
        Point pointSalvo = geometryFactory.createPoint(new Coordinate(-46.633308, -23.550520));
        pointSalvo.setSRID(4326);
        enderecoSalvo.setCoordenadas(pointSalvo);

        cliente = new Cliente();
        cliente.setId(clienteId);
        cliente.setNome("João");
        cliente.setEnderecosSalvos(List.of(enderecoSalvo));

        request = new PedidoRequest();
        request.setCategoria(CategoriaServico.ELETRICA);
        request.setDescricao("Conserto de chuveiro");
        request.setUrgencia(Urgencia.NORMAL);
        request.setOrcamentoEstimado(BigDecimal.valueOf(150.00));
        request.setDataDesejada(LocalDateTime.now().plusDays(2));
        request.setFotos(List.of("foto1"));
    }

    @Test
    void criarPedido_ComEnderecoNovo_DeveConverterLatLonParaPoint() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(pedidoRepository.countByClienteAndStatusIn(any(), any())).thenReturn(0L);

        EnderecoRequest enderecoNovo = new EnderecoRequest();
        enderecoNovo.setLogradouro("Rua Nova");
        enderecoNovo.setNumero("200");
        enderecoNovo.setBairro("Jardim");
        enderecoNovo.setCidade("Rio de Janeiro");
        enderecoNovo.setEstado("RJ");
        enderecoNovo.setCep("20000-000");
        enderecoNovo.setLatitude(-22.9068);
        enderecoNovo.setLongitude(-43.1729);
        request.setEndereco(enderecoNovo);
        request.setEnderecoSalvoIndex(null);

        Pedido pedidoSalvo = new Pedido();
        pedidoSalvo.setId(1L);
        pedidoSalvo.setCategoria(CategoriaServico.ELETRICA);
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedidoSalvo);

        Pedido resultado = pedidoService.criarPedido(clienteId, request);

        assertThat(resultado).isNotNull();
        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        Pedido pedidoCaptured = captor.getValue();

        Endereco enderecoPedido = pedidoCaptured.getEndereco();
        assertThat(enderecoPedido.getLogradouro()).isEqualTo("Rua Nova");
        assertThat(enderecoPedido.getCoordenadas()).isNotNull();
        assertThat(enderecoPedido.getCoordenadas().getX()).isEqualTo(-43.1729);
        assertThat(enderecoPedido.getCoordenadas().getY()).isEqualTo(-22.9068);
        assertThat(enderecoPedido.getCoordenadas().getSRID()).isEqualTo(4326);
    }

    @Test
    void criarPedido_ComEnderecoSalvoIndex_DeveCopiarEnderecoSalvo() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(pedidoRepository.countByClienteAndStatusIn(any(), any())).thenReturn(0L);

        request.setEnderecoSalvoIndex(0);
        request.setEndereco(null);

        Pedido pedidoSalvo = new Pedido();
        pedidoSalvo.setId(1L);
        pedidoSalvo.setCategoria(CategoriaServico.ELETRICA);
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedidoSalvo);

        pedidoService.criarPedido(clienteId, request);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        Endereco enderecoPedido = captor.getValue().getEndereco();

        assertThat(enderecoPedido.getLogradouro()).isEqualTo("Rua Salva");
        assertThat(enderecoPedido.getNumero()).isEqualTo("10");
        assertThat(enderecoPedido.getCoordenadas()).isNotNull();
        assertThat(enderecoPedido.getCoordenadas().getX()).isEqualTo(-46.633308);
        assertThat(enderecoPedido.getCoordenadas().getY()).isEqualTo(-23.550520);
    }

    @Test
    void criarPedido_SemEnderecoNemIndex_DeveLancar400() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        request.setEnderecoSalvoIndex(null);
        request.setEndereco(null);

        assertThatThrownBy(() -> pedidoService.criarPedido(clienteId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("É necessário informar o endereço");
                });
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void criarPedido_IndiceEnderecoSalvoInvalido_DeveLancar400() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        request.setEnderecoSalvoIndex(99);
        request.setEndereco(null);

        assertThatThrownBy(() -> pedidoService.criarPedido(clienteId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("Índice de endereço salvo inválido");
                });
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void criarPedido_ClienteComTresPedidosAbertos_DeveLancar409() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(pedidoRepository.countByClienteAndStatusIn(any(), any())).thenReturn(3L);

        assertThatThrownBy(() -> pedidoService.criarPedido(clienteId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(rse.getReason()).contains("3 pedidos em aberto");
                });
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void criarPedido_ClienteNaoEncontrado_DeveLancar404() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.criarPedido(clienteId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(rse.getReason()).contains("Cliente não encontrado");
                });
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void criarPedido_StatusDeveSerABERTO() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(pedidoRepository.countByClienteAndStatusIn(any(), any())).thenReturn(0L);

        EnderecoRequest enderecoNovo = new EnderecoRequest();
        enderecoNovo.setLogradouro("Rua Teste");
        enderecoNovo.setNumero("1");
        enderecoNovo.setBairro("Bairro");
        enderecoNovo.setCidade("Cidade");
        enderecoNovo.setEstado("SP");
        enderecoNovo.setCep("00000-000");
        request.setEndereco(enderecoNovo);

        Pedido pedidoSalvo = new Pedido();
        pedidoSalvo.setCategoria(CategoriaServico.ELETRICA);
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedidoSalvo);

        pedidoService.criarPedido(clienteId, request);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusPedido.ABERTO);
    }

    @Test
    void criarPedido_DeveAplicarTrimNaDescricaoAntesDePeristir() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(pedidoRepository.countByClienteAndStatusIn(any(), any())).thenReturn(0L);

        request.setDescricao("  Conserto de chuveiro  ");
        EnderecoRequest enderecoNovo = new EnderecoRequest();
        enderecoNovo.setLogradouro("Rua A");
        enderecoNovo.setNumero("1");
        enderecoNovo.setBairro("Centro");
        enderecoNovo.setCidade("São Paulo");
        enderecoNovo.setEstado("SP");
        enderecoNovo.setCep("00000-000");
        request.setEndereco(enderecoNovo);

        Pedido pedidoSalvo = new Pedido();
        pedidoSalvo.setCategoria(CategoriaServico.ELETRICA);
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedidoSalvo);

        pedidoService.criarPedido(clienteId, request);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        assertThat(captor.getValue().getDescricao()).isEqualTo("Conserto de chuveiro");
    }

    @Test
    void criarPedido_DeveSalvarListaDeFotos() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(pedidoRepository.countByClienteAndStatusIn(any(), any())).thenReturn(0L);

        EnderecoRequest enderecoNovo = new EnderecoRequest();
        enderecoNovo.setLogradouro("Rua A");
        enderecoNovo.setNumero("1");
        enderecoNovo.setBairro("Centro");
        enderecoNovo.setCidade("São Paulo");
        enderecoNovo.setEstado("SP");
        enderecoNovo.setCep("00000-000");
        request.setEndereco(enderecoNovo);
        request.setFotos(List.of("foto1", "foto2", "foto3"));

        Pedido pedidoSalvo = new Pedido();
        pedidoSalvo.setCategoria(CategoriaServico.ELETRICA);
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedidoSalvo);

        pedidoService.criarPedido(clienteId, request);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        assertThat(captor.getValue().getFotos()).containsExactly("foto1", "foto2", "foto3");
    }

    @Test
    void listarPedidosComFiltros_quandoProfissionalNaoTemLocalizacao_lancaBadRequest() {
        try (MockedStatic<SecurityContextHolder> securityHolder = mockStatic(SecurityContextHolder.class)) {
            String email = "prof@email.com";
            setupSecurityContextMock(securityHolder, email);

            Profissional profissional = new Profissional();
            profissional.setEmail(email);
            profissional.setLocalizacao(null);
            profissional.setRaioAtuacao(10.0);
            when(profissionalRepository.findByEmail(email)).thenReturn(Optional.of(profissional));

            PedidoFiltroRequest filtro = new PedidoFiltroRequest();
            Pageable pageable = PageRequest.of(0, 10);

            assertThatThrownBy(() -> pedidoService.listarPedidosComFiltros(filtro, pageable))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(rse.getReason()).contains("localização ou raio de atuação");
                    });
        }
    }

    @Test
    void listarPedidosComFiltros_quandoSortByNull_usarMaisRecentes() {
        try (MockedStatic<SecurityContextHolder> securityHolder = mockStatic(SecurityContextHolder.class)) {
            String email = "prof@email.com";
            setupSecurityContextMock(securityHolder, email);

            Point ponto = geometryFactory.createPoint(new Coordinate(-46.6333, -23.5505));
            ponto.setSRID(4326);
            Profissional profissional = new Profissional();
            profissional.setEmail(email);
            profissional.setLocalizacao(ponto);
            profissional.setRaioAtuacao(10.0);
            when(profissionalRepository.findByEmail(email)).thenReturn(Optional.of(profissional));

            PedidoFiltroRequest filtro = new PedidoFiltroRequest();
            Pageable pageable = PageRequest.of(0, 10);
            Pageable expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "criadoEm"));

            Pedido pedido = new Pedido();
            pedido.setId(pedidoId);
            PedidoDistanceRequest dto = new PedidoDistanceRequest(pedido, 5.0);

            Page<PedidoDistanceRequest> mockPage = new PageImpl<>(List.of(dto), expectedPageable, 1L);

            when(pedidoRepository.findWithFiltersAndDistance(eq(ponto), eq(10.0), any(Specification.class), eq(expectedPageable)))
                    .thenReturn(mockPage);
            when(propostaRepository.countByPedidoId(pedidoId)).thenReturn(3);

            Page<PedidoResumoResponse> result = pedidoService.listarPedidosComFiltros(filtro, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(pedidoRepository).findWithFiltersAndDistance(eq(ponto), eq(10.0), any(Specification.class), eq(expectedPageable));
        }
    }

    @Test
    void listarPedidosComFiltros_comSortByMaisRecentes() {
        try (MockedStatic<SecurityContextHolder> securityHolder = mockStatic(SecurityContextHolder.class)) {
            String email = "prof@email.com";
            setupSecurityContextMock(securityHolder, email);

            Point ponto = geometryFactory.createPoint(new Coordinate(-46.6333, -23.5505));
            ponto.setSRID(4326);
            Profissional profissional = new Profissional();
            profissional.setEmail(email);
            profissional.setLocalizacao(ponto);
            profissional.setRaioAtuacao(10.0);
            when(profissionalRepository.findByEmail(email)).thenReturn(Optional.of(profissional));

            PedidoFiltroRequest filtro = new PedidoFiltroRequest();
            filtro.setSortBy(PedidoFiltroRequest.SortBy.MAIS_RECENTES);
            Pageable pageable = PageRequest.of(0, 10);
            Pageable expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "criadoEm"));

            Pedido pedido = new Pedido();
            pedido.setId(pedidoId);
            PedidoDistanceRequest dto = new PedidoDistanceRequest(pedido, 5.0);

            Page<PedidoDistanceRequest> mockPage = new PageImpl<>(List.of(dto), expectedPageable, 1L);

            when(pedidoRepository.findWithFiltersAndDistance(eq(ponto), eq(10.0), any(Specification.class), eq(expectedPageable)))
                    .thenReturn(mockPage);
            when(propostaRepository.countByPedidoId(pedidoId)).thenReturn(2);

            pedidoService.listarPedidosComFiltros(filtro, pageable);

            verify(pedidoRepository).findWithFiltersAndDistance(eq(ponto), eq(10.0), any(Specification.class), eq(expectedPageable));
        }
    }

    @Test
    void listarPedidosComFiltros_comSortByMaisProximos() {
        try (MockedStatic<SecurityContextHolder> securityHolder = mockStatic(SecurityContextHolder.class)) {
            String email = "prof@email.com";
            setupSecurityContextMock(securityHolder, email);

            Point ponto = geometryFactory.createPoint(new Coordinate(-46.6333, -23.5505));
            ponto.setSRID(4326);
            Profissional profesional = new Profissional();
            profesional.setEmail(email);
            profesional.setLocalizacao(ponto);
            profesional.setRaioAtuacao(10.0);
            when(profissionalRepository.findByEmail(email)).thenReturn(Optional.of(profesional));

            PedidoFiltroRequest filtro = new PedidoFiltroRequest();
            filtro.setSortBy(PedidoFiltroRequest.SortBy.MAIS_PROXIMOS);
            Pageable pageable = PageRequest.of(0, 10);
            Pageable expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "distanciaKm"));

            Pedido pedido = new Pedido();
            pedido.setId(pedidoId);
            PedidoDistanceRequest dto = new PedidoDistanceRequest(pedido, 5.0);

            Page<PedidoDistanceRequest> mockPage = new PageImpl<>(List.of(dto), expectedPageable, 1L);

            when(pedidoRepository.findWithFiltersAndDistance(eq(ponto), eq(10.0), any(Specification.class), eq(expectedPageable)))
                    .thenReturn(mockPage);
            when(propostaRepository.countByPedidoId(pedidoId)).thenReturn(2);

            pedidoService.listarPedidosComFiltros(filtro, pageable);

            verify(pedidoRepository).findWithFiltersAndDistance(eq(ponto), eq(10.0), any(Specification.class), eq(expectedPageable));
        }
    }

    @Test
    void listarPedidosComFiltros_comSortByUrgentes() {
        try (MockedStatic<SecurityContextHolder> securityHolder = mockStatic(SecurityContextHolder.class)) {
            String email = "prof@email.com";
            setupSecurityContextMock(securityHolder, email);

            Point ponto = geometryFactory.createPoint(new Coordinate(-46.6333, -23.5505));
            ponto.setSRID(4326);
            Profissional profissional = new Profissional();
            profissional.setEmail(email);
            profissional.setLocalizacao(ponto);
            profissional.setRaioAtuacao(10.0);
            when(profissionalRepository.findByEmail(email)).thenReturn(Optional.of(profissional));

            PedidoFiltroRequest filtro = new PedidoFiltroRequest();
            filtro.setSortBy(PedidoFiltroRequest.SortBy.URGENTES);
            Pageable pageable = PageRequest.of(0, 10);
            Sort expectedSort = Sort.by(Sort.Direction.DESC, "urgente").and(Sort.by(Sort.Direction.DESC, "criadoEm"));
            Pageable expectedPageable = PageRequest.of(0, 10, expectedSort);

            Pedido pedido = new Pedido();
            pedido.setId(pedidoId);
            PedidoDistanceRequest dto = new PedidoDistanceRequest(pedido, 5.0);

            Page<PedidoDistanceRequest> mockPage = new PageImpl<>(List.of(dto), expectedPageable, 1L);

            when(pedidoRepository.findWithFiltersAndDistance(eq(ponto), eq(10.0), any(Specification.class), eq(expectedPageable)))
                    .thenReturn(mockPage);
            when(propostaRepository.countByPedidoId(pedidoId)).thenReturn(2);

            pedidoService.listarPedidosComFiltros(filtro, pageable);

            verify(pedidoRepository).findWithFiltersAndDistance(eq(ponto), eq(10.0), any(Specification.class), eq(expectedPageable));
        }
    }

    @Test
    void listarPedidosComFiltros_aplicaFiltrosCategoriaEUrgente() {
        try (MockedStatic<SecurityContextHolder> securityHolder = mockStatic(SecurityContextHolder.class)) {
            String email = "prof@email.com";
            setupSecurityContextMock(securityHolder, email);

            Point ponto = geometryFactory.createPoint(new Coordinate(-46.6333, -23.5505));
            ponto.setSRID(4326);
            Profissional profissional = new Profissional();
            profissional.setEmail(email);
            profissional.setLocalizacao(ponto);
            profissional.setRaioAtuacao(10.0);
            when(profissionalRepository.findByEmail(email)).thenReturn(Optional.of(profissional));

            PedidoFiltroRequest filtro = new PedidoFiltroRequest();
            filtro.setCategoria(CategoriaServico.ELETRICA);
            filtro.setUrgente(true);
            filtro.setSortBy(PedidoFiltroRequest.SortBy.MAIS_RECENTES);
            Pageable pageable = PageRequest.of(0, 10);
            Pageable expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "criadoEm"));

            Pedido pedido = new Pedido();
            pedido.setId(pedidoId);
            PedidoDistanceRequest dto = new PedidoDistanceRequest(pedido, 5.0);

            Page<PedidoDistanceRequest> mockPage = new PageImpl<>(List.of(dto), expectedPageable, 1L);

            when(pedidoRepository.findWithFiltersAndDistance(eq(ponto), eq(10.0), any(Specification.class), eq(expectedPageable)))
                    .thenReturn(mockPage);
            when(propostaRepository.countByPedidoId(pedidoId)).thenReturn(1);

            pedidoService.listarPedidosComFiltros(filtro, pageable);

            ArgumentCaptor<Specification<Pedido>> specCaptor = ArgumentCaptor.forClass(Specification.class);
            verify(pedidoRepository).findWithFiltersAndDistance(eq(ponto), eq(10.0), specCaptor.capture(), eq(expectedPageable));
            assertThat(specCaptor.getValue()).isNotNull();
        }
    }

    @Test
    void listarPedidosComFiltros_contaPropostasCorretamente() {
        try (MockedStatic<SecurityContextHolder> securityHolder = mockStatic(SecurityContextHolder.class)) {
            String email = "prof@email.com";
            setupSecurityContextMock(securityHolder, email);

            Point ponto = geometryFactory.createPoint(new Coordinate(-46.6333, -23.5505));
            ponto.setSRID(4326);
            Profissional profissional = new Profissional();
            profissional.setEmail(email);
            profissional.setLocalizacao(ponto);
            profissional.setRaioAtuacao(10.0);
            when(profissionalRepository.findByEmail(email)).thenReturn(Optional.of(profissional));

            PedidoFiltroRequest filtro = new PedidoFiltroRequest();
            filtro.setSortBy(PedidoFiltroRequest.SortBy.MAIS_RECENTES);
            Pageable pageable = PageRequest.of(0, 10);
            Pageable expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "criadoEm"));

            Pedido pedido1 = new Pedido();
            pedido1.setId(pedidoId);
            Pedido pedido2 = new Pedido();
            pedido2.setId(pedidoId2);
            PedidoDistanceRequest dto1 = new PedidoDistanceRequest(pedido1, 3.0);
            PedidoDistanceRequest dto2 = new PedidoDistanceRequest(pedido2, 7.0);

            Page<PedidoDistanceRequest> mockPage = new PageImpl<>(List.of(dto1, dto2), expectedPageable, 2L);

            when(pedidoRepository.findWithFiltersAndDistance(eq(ponto), eq(10.0), any(Specification.class), eq(expectedPageable)))
                    .thenReturn(mockPage);
            when(propostaRepository.countByPedidoId(pedidoId)).thenReturn(5);
            when(propostaRepository.countByPedidoId(pedidoId2)).thenReturn(2);

            Page<PedidoResumoResponse> result = pedidoService.listarPedidosComFiltros(filtro, pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).contadorPropostas()).isEqualTo(5);
            assertThat(result.getContent().get(1).contadorPropostas()).isEqualTo(2);
            verify(propostaRepository, times(1)).countByPedidoId(pedidoId);
            verify(propostaRepository, times(1)).countByPedidoId(pedidoId2);
        }
    }

    @Test
    void listarPedidosComFiltros_quandoNaoHaPedidos_retornaPaginaVazia() {
        try (MockedStatic<SecurityContextHolder> securityHolder = mockStatic(SecurityContextHolder.class)) {
            String email = "prof@email.com";
            setupSecurityContextMock(securityHolder, email);

            Point ponto = geometryFactory.createPoint(new Coordinate(-46.6333, -23.5505));
            ponto.setSRID(4326);
            Profissional profissional = new Profissional();
            profissional.setEmail(email);
            profissional.setLocalizacao(ponto);
            profissional.setRaioAtuacao(10.0);
            when(profissionalRepository.findByEmail(email)).thenReturn(Optional.of(profissional));

            PedidoFiltroRequest filtro = new PedidoFiltroRequest();
            Pageable pageable = PageRequest.of(0, 10);
            Pageable expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "criadoEm"));

            Page<PedidoDistanceRequest> mockPage = new PageImpl<>(Collections.emptyList(), expectedPageable, 0L);

            when(pedidoRepository.findWithFiltersAndDistance(eq(ponto), eq(10.0), any(Specification.class), eq(expectedPageable)))
                    .thenReturn(mockPage);

            Page<PedidoResumoResponse> result = pedidoService.listarPedidosComFiltros(filtro, pageable);

            assertThat(result).isEmpty();
            verify(propostaRepository, never()).countByPedidoId(anyLong());
        }
    }

    @Test
    void listarPedidosComFiltros_UsuarioNaoAutenticado_DeveLancar401() {
        try (MockedStatic<SecurityContextHolder> securityHolder = mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(null);
            securityHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            PedidoFiltroRequest filtro = new PedidoFiltroRequest();
            Pageable pageable = PageRequest.of(0, 10);

            assertThatThrownBy(() -> pedidoService.listarPedidosComFiltros(filtro, pageable))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                        assertThat(rse.getReason()).contains("Usuário não autenticado");
                    });
        }
    }

    @Test
    void listarPedidosComFiltros_ProfissionalNaoEncontradoNoBanco_DeveLancar404() {
        try (MockedStatic<SecurityContextHolder> securityHolder = mockStatic(SecurityContextHolder.class)) {
            String email = "fantasma@email.com";
            setupSecurityContextMock(securityHolder, email);

            when(profissionalRepository.findByEmail(email)).thenReturn(Optional.empty());

            PedidoFiltroRequest filtro = new PedidoFiltroRequest();
            Pageable pageable = PageRequest.of(0, 10);

            assertThatThrownBy(() -> pedidoService.listarPedidosComFiltros(filtro, pageable))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(rse.getReason()).contains("Profissional não encontrado");
                    });
        }
    }

    @Test
    void aceitarProposta_ComDadosValidos_DeveAtualizarStatusEDispararMocks() {
        Pedido pedido = new Pedido();
        pedido.setId(10L);
        pedido.setCliente(cliente);
        pedido.setStatus(StatusPedido.ABERTO);

        Profissional profVencedor = new Profissional();
        profVencedor.setId(100L);
        profVencedor.setNome("Carlos Silva");

        Profissional profPerdedor = new Profissional();
        profPerdedor.setId(200L);

        Proposta propostaEscolhida = new Proposta();
        propostaEscolhida.setId(50L);
        propostaEscolhida.setPedido(pedido);
        propostaEscolhida.setProfissional(profVencedor);
        propostaEscolhida.setStatus(StatusProposta.ATIVA);

        Proposta propostaRecusada = new Proposta();
        propostaRecusada.setId(51L);
        propostaRecusada.setPedido(pedido);
        propostaRecusada.setProfissional(profPerdedor);
        propostaRecusada.setStatus(StatusProposta.ATIVA);

        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));
        when(propostaRepository.findById(50L)).thenReturn(Optional.of(propostaEscolhida));
        when(propostaRepository.findByPedidoId(10L)).thenReturn(List.of(propostaEscolhida, propostaRecusada));

        var resposta = pedidoService.aceitarProposta(10L, 50L, clienteId);

        assertThat(resposta).isNotNull();
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.EM_ANDAMENTO);
        assertThat(pedido.getProfissionalAtribuido()).isEqualTo(profVencedor);
        assertThat(propostaEscolhida.getStatus()).isEqualTo(StatusProposta.ACEITA);
        assertThat(propostaRecusada.getStatus()).isEqualTo(StatusProposta.RECUSADA);

        verify(pedidoRepository).save(pedido);
    }

    @Test
    void aceitarProposta_UsuarioNaoForDonoDoPedido_DeveLancar403() {
        Pedido pedido = new Pedido();
        pedido.setId(10L);
        Cliente outroCliente = new Cliente();
        outroCliente.setId(999L); // ID diferente do cliente autenticado
        pedido.setCliente(outroCliente);

        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.aceitarProposta(10L, 50L, clienteId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(rse.getReason()).contains("não é o dono do pedido");
                });

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void aceitarProposta_PedidoNaoEstaAberto_DeveLancar422() {
        Pedido pedido = new Pedido();
        pedido.setId(10L);
        pedido.setCliente(cliente);
        pedido.setStatus(StatusPedido.EM_ANDAMENTO);

        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.aceitarProposta(10L, 50L, clienteId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(rse.getReason()).contains("não está com status ABERTO");
                });
    }

    @Test
    void aceitarProposta_PropostaNaoPertenceAoPedido_DeveLancar404() {
        Pedido pedido = new Pedido();
        pedido.setId(10L);
        pedido.setCliente(cliente);
        pedido.setStatus(StatusPedido.ABERTO);

        Pedido outroPedido = new Pedido();
        outroPedido.setId(99L); // ID de pedido diferente

        Proposta propostaIncompativel = new Proposta();
        propostaIncompativel.setId(50L);
        propostaIncompativel.setPedido(outroPedido);

        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));
        when(propostaRepository.findById(50L)).thenReturn(Optional.of(propostaIncompativel));

        assertThatThrownBy(() -> pedidoService.aceitarProposta(10L, 50L, clienteId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(rse.getReason()).contains("não pertence a este pedido");
                });
    }

    @Test
    void listarMeusPedidos_SemFiltroStatus_DeveUsarQuerySemStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        when(pedidoRepository.findByClienteId(clienteId, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var result = pedidoService.listarMeusPedidos(clienteId, null, pageable);

        assertThat(result).isEmpty();
        verify(pedidoRepository).findByClienteId(clienteId, pageable);
        verify(pedidoRepository, never()).findByClienteIdAndStatus(any(), any(), any());
    }

    @Test
    void listarMeusPedidos_ComFiltroStatus_DeveUsarQueryComStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        when(pedidoRepository.findByClienteIdAndStatusIn(clienteId, List.of(StatusPedido.ABERTO), pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var result = pedidoService.listarMeusPedidos(clienteId, List.of(StatusPedido.ABERTO), pageable);

        assertThat(result).isEmpty();
        verify(pedidoRepository).findByClienteIdAndStatusIn(clienteId, List.of(StatusPedido.ABERTO), pageable);
        verify(pedidoRepository, never()).findByClienteId(any(), any());
    }

    @Test
    void listarMeusPedidos_DeveMapearPedidoParaResponseCorretamente() {
        Pedido pedido = new Pedido();
        pedido.setId(42L);
        pedido.setStatus(StatusPedido.ABERTO);
        pedido.setCategoria(CategoriaServico.ELETRICA);
        pedido.setDescricao("Troca de tomada");
        pedido.setUrgencia(Urgencia.NORMAL);
        pedido.setOrcamentoEstimado(BigDecimal.valueOf(200.00));
        pedido.setFotos(List.of());
        pedido.setCliente(cliente);

        Pageable pageable = PageRequest.of(0, 10);
        when(pedidoRepository.findByClienteId(clienteId, pageable))
                .thenReturn(new PageImpl<>(List.of(pedido), pageable, 1));

        var result = pedidoService.listarMeusPedidos(clienteId, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        var response = result.getContent().get(0);
        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getStatus()).isEqualTo(StatusPedido.ABERTO);
        assertThat(response.getCategoria()).isEqualTo(CategoriaServico.ELETRICA);
        assertThat(response.getClienteId()).isEqualTo(clienteId);
        assertThat(response.getClienteNome()).isEqualTo("João");
    }

    @Test
    void listarMeusPedidos_ComMultiplosPedidos_DeveRetornarPaginaComTodos() {
        Pedido p1 = new Pedido();
        p1.setId(1L);
        p1.setStatus(StatusPedido.ABERTO);
        p1.setCliente(cliente);
        p1.setFotos(List.of());

        Pedido p2 = new Pedido();
        p2.setId(2L);
        p2.setStatus(StatusPedido.CONCLUIDO);
        p2.setCliente(cliente);
        p2.setFotos(List.of());

        Pageable pageable = PageRequest.of(0, 10);
        when(pedidoRepository.findByClienteId(clienteId, pageable))
                .thenReturn(new PageImpl<>(List.of(p1, p2), pageable, 2));

        var result = pedidoService.listarMeusPedidos(clienteId, null, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(result.getContent().get(1).getId()).isEqualTo(2L);
    }

    @Test
    void listarPropostasAtivas_DeveRetornarApenasPendentesOrdenadas() {
        Pedido pedido = new Pedido();
        pedido.setId(10L);
        pedido.setCliente(cliente);

        Profissional p1 = new Profissional();
        p1.setNotaMedia(4.5);
        Proposta prop1 = new Proposta();
        prop1.setValorOferecido(BigDecimal.valueOf(200.00));
        prop1.setProfissional(p1);

        Profissional p2 = new Profissional();
        p2.setNotaMedia(5.0);
        Proposta prop2 = new Proposta();
        prop2.setValorOferecido(BigDecimal.valueOf(100.00));
        prop2.setProfissional(p2);

        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));
        when(propostaRepository.findByPedidoIdAndStatus(10L, StatusProposta.ATIVA))
                .thenReturn(new ArrayList<>(List.of(prop1, prop2)));

        var resultadoPreco = pedidoService.listarPropostasAtivas(10L, clienteId, "preco");

        assertThat(resultadoPreco).hasSize(2);
        assertThat(resultadoPreco.get(0).valorProposto()).isEqualTo(BigDecimal.valueOf(100.00)); // Menor preço primeiro
    }

    @Test
    void notificarProfissionaisDaCategoria_DeveNotificarApenasProfissionaisDentroDoRaioDe10Km() {
        Pedido pedido = new Pedido();
        pedido.setCategoria(CategoriaServico.ELETRICA);
        Endereco endPedido = new Endereco();
        Point coordPedido = geometryFactory.createPoint(new Coordinate(-46.633308, -23.550520));
        coordPedido.setSRID(4326);
        endPedido.setCoordenadas(coordPedido);
        pedido.setEndereco(endPedido);

        Profissional profissionalPerto = new Profissional();
        profissionalPerto.setId(10L);
        profissionalPerto.setNome("Jorge Eletricista");
        Point coordPerto = geometryFactory.createPoint(new Coordinate(-46.638000, -23.559000));
        coordPerto.setSRID(4326);
        profissionalPerto.setLocalizacao(coordPerto);

        Profissional profissionalLonge = new Profissional();
        profissionalLonge.setId(11L);
        profissionalLonge.setNome("Marcos Eletricista");
        Point coordLonge = geometryFactory.createPoint(new Coordinate(-46.400000, -23.400000));
        coordLonge.setSRID(4326);
        profissionalLonge.setLocalizacao(coordLonge);

        when(profissionalRepository.findByCategoriasAtendidasAndAtivoTrueAndPerfilCompletoTrue(CategoriaServico.ELETRICA))
                .thenReturn(List.of(profissionalPerto, profissionalLonge));

        pedidoService.notificarProfissionaisDaCategoria(pedido);

        verify(pushNotificationService, times(1)).enviarNotificacaoNovoPedido(profissionalPerto, pedido);
        verify(pushNotificationService, never()).enviarNotificacaoNovoPedido(profissionalLonge, pedido);
    }

    @Test
    void listarPedidosDoCliente_SemFiltroStatus_DeveChamarFindByClienteIdEMapearCards() {
        Pageable pageable = PageRequest.of(0, 10);
        Pedido pedido = new Pedido();
        pedido.setId(100L);
        pedido.setCategoria(CategoriaServico.ELETRICA);
        pedido.setStatus(StatusPedido.ABERTO);
        pedido.setDescricao("Chuveiro queimou totalmente e preciso de reparos urgentes na fiação.");
        pedido.setEndereco(enderecoSalvo);
        pedido.setDataDesejada(LocalDateTime.of(2026, 5, 20, 14, 0));

        when(pedidoRepository.findByClienteId(clienteId, pageable))
                .thenReturn(new PageImpl<>(List.of(pedido), pageable, 1));

        Page<PedidoCardDTO> resultado = pedidoService.listarPedidosDoCliente(clienteId, null, pageable);

        assertThat(resultado).isNotEmpty();
        PedidoCardDTO card = resultado.getContent().get(0);
        assertThat(card.getId()).isEqualTo(100L);
        assertThat(card.getTitulo()).isEqualTo("Elétrica");
        assertThat(card.getStatus()).isEqualTo(StatusPedido.ABERTO);
        assertThat(card.getData()).isEqualTo("20/05/2026");
        assertThat(card.getPeriodo()).isEqualTo("Tarde");
        assertThat(card.getEndereco()).contains("Rua Salva, 10");
        assertThat(card.getProfissionalNome()).isNull();

        verify(pedidoRepository).findByClienteId(clienteId, pageable);
        verify(pedidoRepository, never()).findByClienteIdAndStatus(anyLong(), any(), any());
    }

    @Test
    void listarPedidosDoCliente_QuandoHorarioForMeiaNoite_DeveRetornarPeriodoACombinar() {
        Pageable pageable = PageRequest.of(0, 10);
        Pedido pedido = new Pedido();
        pedido.setCategoria(CategoriaServico.PINTURA);
        pedido.setStatus(StatusPedido.ABERTO);
        pedido.setDataDesejada(LocalDateTime.of(2026, 5, 20, 0, 0));

        when(pedidoRepository.findByClienteId(clienteId, pageable))
                .thenReturn(new PageImpl<>(List.of(pedido), pageable, 1));

        Page<PedidoCardDTO> resultado = pedidoService.listarPedidosDoCliente(clienteId, null, pageable);

        assertThat(resultado).isNotEmpty();
        PedidoCardDTO card = resultado.getContent().get(0);
        assertThat(card.getPeriodo()).isEqualTo("A combinar");
    }

    @Test
    void listarPedidosDoCliente_ComFiltroStatus_DeveChamarFindByClienteIdAndStatusEMapearCamposDeSeguranca() {
        Pageable pageable = PageRequest.of(0, 10);
        Profissional prestador = new Profissional();
        prestador.setNome("Fernando Pintor");

        Pedido pedido = new Pedido();
        pedido.setId(101L);
        pedido.setCategoria(CategoriaServico.PINTURA);
        pedido.setStatus(StatusPedido.EM_ANDAMENTO);
        pedido.setDescricao("Pintura de duas salas comerciais.");
        pedido.setProfissionalAtribuido(prestador);
        pedido.setEndereco(enderecoSalvo);

        when(pedidoRepository.findByClienteIdAndStatus(clienteId, StatusPedido.EM_ANDAMENTO, pageable))
                .thenReturn(new PageImpl<>(List.of(pedido), pageable, 1));

        Page<PedidoCardDTO> resultado = pedidoService.listarPedidosDoCliente(clienteId, StatusPedido.EM_ANDAMENTO, pageable);

        assertThat(resultado).isNotEmpty();
        PedidoCardDTO card = resultado.getContent().get(0);
        assertThat(card.getId()).isEqualTo(101L);
        assertThat(card.getStatus()).isEqualTo(StatusPedido.EM_ANDAMENTO);
        assertThat(card.getProfissionalNome()).isEqualTo("Fernando Pintor");
        assertThat(card.getEndereco()).isNull();

        verify(pedidoRepository).findByClienteIdAndStatus(clienteId, StatusPedido.EM_ANDAMENTO, pageable);
        verify(pedidoRepository, never()).findByClienteId(anyLong(), any());
    }

    @Test
    @DisplayName("Deve atualizar pedido com sucesso alterando apenas os três campos permitidos e notificando profissionais")
    void deveAtualizarPedidoComSucesso() {
        Long pedidoId = 1L;
        Long clienteId = 10L;

        Cliente cliente = new Cliente();
        cliente.setId(clienteId);

        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setCliente(cliente);
        pedido.setStatus(StatusPedido.ABERTO);
        pedido.setDescricao("Descricao antiga longa de teste");

        Profissional prof = new Profissional();
        prof.setId(20L);
        prof.setTokenFcm("token-fcm-profissional");

        Proposta proposta = new Proposta();
        proposta.setProfissional(prof);
        proposta.setStatus(StatusProposta.ATIVA);

        PedidoEditarRequest request = new PedidoEditarRequest("Nova descricao com tamanho valido de teste", List.of("foto1.png"), java.time.LocalDateTime.now().plusDays(2));

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));
        when(propostaRepository.findByPedidoIdAndStatus(pedidoId, StatusProposta.ATIVA)).thenReturn(List.of(proposta));

        PedidoResponse response = pedidoService.atualizarPedido(pedidoId, clienteId, request);

        assertNotNull(response);
        assertEquals("Nova descricao com tamanho valido de teste", pedido.getDescricao());
        verify(pushNotificationService, times(1)).enviarNotificacao(eq("token-fcm-profissional"), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve lançar 422 Unprocessable Entity quando o status do pedido não for ABERTO")
    void deveLancarErroQuandoStatusNaoForAberto() {
        Long pedidoId = 1L;
        Long clienteId = 10L;

        Cliente cliente = new Cliente();
        cliente.setId(clienteId);

        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setCliente(cliente);
        pedido.setStatus(StatusPedido.EM_ANDAMENTO); // Bloqueia edição

        PedidoEditarRequest request = new PedidoEditarRequest("Nova descricao com tamanho valido de teste", List.of(), null);
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                pedidoService.atualizarPedido(pedidoId, clienteId, request)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar 403 Forbidden quando usuário solicitante não for o dono do pedido")
    void deveLancarErroQuandoUsuarioNaoForDono() {
        Long pedidoId = 1L;
        Long clienteIdDono = 10L;
        Long clienteIdInvasor = 99L;

        Cliente cliente = new Cliente();
        cliente.setId(clienteIdDono);

        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setCliente(cliente);
        pedido.setStatus(StatusPedido.ABERTO);

        PedidoEditarRequest request = new PedidoEditarRequest("Nova descricao com tamanho valido de teste", List.of(), null);
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                pedidoService.atualizarPedido(pedidoId, clienteIdInvasor, request)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Não deve disparar notificações se não houver propostas ativas")
    void naoDeveNotificarSeNaoHouverPropostasAtivas() {
        Long pedidoId = 1L;
        Long clienteId = 10L;

        Cliente cliente = new Cliente();
        cliente.setId(clienteId);

        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setCliente(cliente);
        pedido.setStatus(StatusPedido.ABERTO);
        pedido.setDescricao("Descricao antiga longa de teste");

        PedidoEditarRequest request = new PedidoEditarRequest("Nova descricao com tamanho valido de teste", List.of(), null);

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));
        when(propostaRepository.findByPedidoIdAndStatus(pedidoId, StatusProposta.ATIVA)).thenReturn(List.of());

        pedidoService.atualizarPedido(pedidoId, clienteId, request);

        verifyNoInteractions(pushNotificationService);
    }
    // ── UC-10: marcarComoConcluido ──────────────────────────────────────────

    @Test
    void marcarComoConcluido_Sucesso_DeveTransicionarParaAguardandoValidacaoENotificarCliente() {
        Profissional profissional = new Profissional();
        profissional.setId(10L);

        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setStatus(StatusPedido.EM_ANDAMENTO);
        pedido.setCliente(cliente);
        pedido.setProfissionalAtribuido(profissional);

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConclusaoRequestDTO dto = new ConclusaoRequestDTO();
        dto.setFotoConclusaoUrl("https://storage/foto.jpg");

        ConfirmacaoResponseDTO response = pedidoService.marcarComoConcluido(pedidoId, 10L, dto);

        assertThat(response.getStatus()).isEqualTo(StatusPedido.AGUARDANDO_VALIDACAO);
        assertThat(response.getConcluidoEm()).isNotNull();
        assertThat(response.getPrazoAutoConfirmacaoEm()).isEqualTo(response.getConcluidoEm().plusHours(48));
        assertThat(response.getFotoConclusaoUrl()).isEqualTo("https://storage/foto.jpg");
        verify(chatService).encerrarCanal(pedidoId);
    }

    @Test
    void marcarComoConcluido_ProfissionalDiferente_DeveLancar403() {
        Profissional outroProfissional = new Profissional();
        outroProfissional.setId(99L);

        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setStatus(StatusPedido.EM_ANDAMENTO);
        pedido.setCliente(cliente);
        pedido.setProfissionalAtribuido(outroProfissional);

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.marcarComoConcluido(pedidoId, 10L, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void marcarComoConcluido_StatusErrado_DeveLancar422() {
        Profissional profissional = new Profissional();
        profissional.setId(10L);

        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setStatus(StatusPedido.ABERTO);
        pedido.setCliente(cliente);
        pedido.setProfissionalAtribuido(profissional);

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.marcarComoConcluido(pedidoId, 10L, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    // ── UC-10: confirmarConclusao ───────────────────────────────────────────

    @Test
    void confirmarConclusao_Sucesso_DeveTransicionarParaConcluido_EFecharCanal() {
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setStatus(StatusPedido.AGUARDANDO_VALIDACAO);
        pedido.setCliente(cliente);
        pedido.setConcluidoEm(LocalDateTime.now().minusHours(1));

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConfirmacaoResponseDTO response = pedidoService.confirmarConclusao(pedidoId, clienteId, false);

        assertThat(response.getStatus()).isEqualTo(StatusPedido.CONCLUIDO);
        verify(chatService).fecharCanal(pedidoId);
    }

    @Test
    void confirmarConclusao_ClienteErrado_DeveLancar403() {
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setStatus(StatusPedido.AGUARDANDO_VALIDACAO);
        pedido.setCliente(cliente);

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.confirmarConclusao(pedidoId, 999L, false))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(chatService, never()).fecharCanal(any());
    }

    @Test
    void confirmarConclusao_StatusErrado_DeveLancar422() {
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setStatus(StatusPedido.CONCLUIDO);
        pedido.setCliente(cliente);

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.confirmarConclusao(pedidoId, clienteId, false))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void confirmarConclusao_AutoConfirmado_IgnoraChecagemDeCliente() {
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setStatus(StatusPedido.AGUARDANDO_VALIDACAO);
        pedido.setCliente(cliente);
        pedido.setConcluidoEm(LocalDateTime.now().minusHours(50));

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConfirmacaoResponseDTO response = pedidoService.confirmarConclusao(pedidoId, null, true);

        assertThat(response.getStatus()).isEqualTo(StatusPedido.CONCLUIDO);
        verify(chatService).fecharCanal(pedidoId);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        assertThat(captor.getValue().getAutoConfirmado()).isTrue();
    }

    // ── UC-10: reportarProblema ─────────────────────────────────────────────

    @Test
    void reportarProblema_Sucesso_DeveTransicionarParaEmDisputa_EReabrirCanal() {
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setStatus(StatusPedido.AGUARDANDO_VALIDACAO);
        pedido.setCliente(cliente);
        pedido.setConcluidoEm(LocalDateTime.now().minusHours(2));

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConfirmacaoResponseDTO response = pedidoService.reportarProblema(pedidoId, clienteId);

        assertThat(response.getStatus()).isEqualTo(StatusPedido.EM_DISPUTA);
        verify(chatService).reabrirCanalParaDisputa(pedidoId);
    }

    @Test
    void reportarProblema_ClienteErrado_DeveLancar403() {
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setStatus(StatusPedido.AGUARDANDO_VALIDACAO);
        pedido.setCliente(cliente);

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.reportarProblema(pedidoId, 999L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(chatService, never()).reabrirCanalParaDisputa(any());
    }

    @Test
    void reportarProblema_StatusErrado_DeveLancar422() {
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setStatus(StatusPedido.EM_ANDAMENTO);
        pedido.setCliente(cliente);

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.reportarProblema(pedidoId, clienteId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }
}