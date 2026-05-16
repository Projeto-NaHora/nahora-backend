package com.nahora.services;

import com.nahora.dto.request.EnderecoRequest;
import com.nahora.dto.request.PedidoRequest;
import com.nahora.model.*;
import com.nahora.model.enums.CategoriaServico;
import com.nahora.model.enums.StatusPedido;
import com.nahora.model.enums.StatusProposta;
import com.nahora.model.enums.Urgencia;
import com.nahora.repositories.ClienteRepository;
import com.nahora.repositories.PedidoRepository;
import com.nahora.repositories.PropostaRepository;
import org.junit.jupiter.api.BeforeEach;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private PropostaRepository propostaRepository;

    @InjectMocks
    private PedidoService pedidoService;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private Cliente cliente;
    private PedidoRequest request;
    private Endereco enderecoSalvo;
    private final Long clienteId = 1L; // ID fixo para os testes

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
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedidoSalvo);

        Pedido resultado = pedidoService.criarPedido(clienteId, request);

        assertThat(resultado).isNotNull();
        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        Pedido pedidoCapturado = captor.getValue();

        Endereco enderecoPedido = pedidoCapturado.getEndereco();
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
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedidoSalvo);

        pedidoService.criarPedido(clienteId, request);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusPedido.ABERTO);
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
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedidoSalvo);

        pedidoService.criarPedido(clienteId, request);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        assertThat(captor.getValue().getFotos()).containsExactly("foto1", "foto2", "foto3");
    }

    @Test
    void aceitarProposta_ComDadosValidos_DeveAtualizarStatusEDispararMocks() {
        // Arrange
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
        propostaEscolhida.setStatus(StatusProposta.PENDENTE);
        propostaEscolhida.setHorariosDisponiveis(List.of());

        Proposta propostaRecusada = new Proposta();
        propostaRecusada.setId(51L);
        propostaRecusada.setPedido(pedido);
        propostaRecusada.setProfissional(profPerdedor);
        propostaRecusada.setStatus(StatusProposta.PENDENTE);

        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));
        when(propostaRepository.findById(50L)).thenReturn(Optional.of(propostaEscolhida));
        when(propostaRepository.findByPedidoId(10L)).thenReturn(List.of(propostaEscolhida, propostaRecusada));

        // Act
        var resposta = pedidoService.aceitarProposta(10L, 50L, clienteId);

        // Assert
        assertThat(resposta).isNotNull();
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.EM_ANDAMENTO);
        assertThat(pedido.getProfissionalAtribuido()).isEqualTo(profVencedor);
        assertThat(propostaEscolhida.getStatus()).isEqualTo(StatusProposta.ACEITA);
        assertThat(propostaRecusada.getStatus()).isEqualTo(StatusProposta.REJEITADA);

        verify(pedidoRepository).save(pedido);
    }

    @Test
    void aceitarProposta_UsuarioNaoForDonoDoPedido_DeveLancar403() {
        // Arrange
        Pedido pedido = new Pedido();
        pedido.setId(10L);
        Cliente outroCliente = new Cliente();
        outroCliente.setId(999L); // ID diferente do cliente autenticado
        pedido.setCliente(outroCliente);

        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));

        // Act & Assert
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
        // Arrange
        Pedido pedido = new Pedido();
        pedido.setId(10L);
        pedido.setCliente(cliente);
        pedido.setStatus(StatusPedido.EM_ANDAMENTO); // Não está ABERTO

        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));

        // Act & Assert
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
        // Arrange
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

        // Act & Assert
        assertThatThrownBy(() -> pedidoService.aceitarProposta(10L, 50L, clienteId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(rse.getReason()).contains("não pertence a este pedido");
                });
    }

    @Test
    void listarPropostasAtivas_DeveRetornarApenasPendentesOrdenadas() {
        // Arrange
        Pedido pedido = new Pedido();
        pedido.setId(10L);
        pedido.setCliente(cliente);

        Profissional p1 = new Profissional();
        p1.setNotaMedia(4.5);
        Proposta prop1 = new Proposta();
        prop1.setValor(BigDecimal.valueOf(200.00));
        prop1.setProfissional(p1);
        prop1.setHorariosDisponiveis(List.of());

        Profissional p2 = new Profissional();
        p2.setNotaMedia(5.0);
        Proposta prop2 = new Proposta();
        prop2.setValor(BigDecimal.valueOf(100.00)); // Mais barata e maior nota
        prop2.setProfissional(p2);
        prop2.setHorariosDisponiveis(List.of());

        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));
        when(propostaRepository.findByPedidoIdAndStatus(10L, StatusProposta.PENDENTE))
                .thenReturn(new ArrayList<>(List.of(prop1, prop2)));

        // Act
        var resultadoPreco = pedidoService.listarPropostasAtivas(10L, clienteId, "preco");

        // Assert
        assertThat(resultadoPreco).hasSize(2);
        assertThat(resultadoPreco.get(0).valorProposto()).isEqualTo(BigDecimal.valueOf(100.00)); // Menor preço primeiro
    }
}