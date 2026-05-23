package com.nahora.services;

import com.nahora.dto.request.HorarioPropostaDTO;
import com.nahora.dto.request.PropostaRequestDTO;
import com.nahora.model.*;
import com.nahora.model.enums.StatusPedido;
import com.nahora.model.enums.StatusProposta;
import com.nahora.repositories.PedidoRepository;
import com.nahora.repositories.ProfissionalRepository;
import com.nahora.repositories.PropostaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class PropostaServiceTest {

    @Mock
    private PropostaRepository propostaRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ProfissionalRepository profissionalRepository;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private PropostaService propostaService;

    private Pedido pedido;
    private Cliente cliente;
    private Profissional profissional;
    private PropostaRequestDTO requestDTO;

    private static final Long PEDIDO_ID = 1L;
    private static final Long PROFISSIONAL_ID = 2L;
    private static final Long CLIENTE_ID = 10L;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(CLIENTE_ID);

        pedido = new Pedido();
        pedido.setId(PEDIDO_ID);
        pedido.setCliente(cliente);
        pedido.setStatus(StatusPedido.ABERTO);

        profissional = new Profissional();
        profissional.setId(PROFISSIONAL_ID);

        requestDTO = new PropostaRequestDTO();
        requestDTO.setValorOferecido(BigDecimal.valueOf(250.00));
        requestDTO.setDescricao("Serviço completo com garantia");
        requestDTO.setHorariosDisponiveis(List.of(
                horario(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2)),
                horario(LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(2))
        ));
    }

    // --- Happy path: criação ---

    @Test
    void salvar_SemPropostaExistente_DeveCriarNovaProposta() {
        when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(profissionalRepository.findById(PROFISSIONAL_ID)).thenReturn(Optional.of(profissional));
        when(propostaRepository.findByPedidoIdAndProfissionalIdAndStatus(PEDIDO_ID, PROFISSIONAL_ID, StatusProposta.ATIVA))
                .thenReturn(Optional.empty());

        Proposta salva = propostaSalva();
        when(propostaRepository.save(any(Proposta.class))).thenReturn(salva);

        PropostaService.PropostaSalvaResult resultado =
                propostaService.salvarOuAtualizarProposta(PEDIDO_ID, PROFISSIONAL_ID, requestDTO);

        assertThat(resultado.criada()).isTrue();
        verify(propostaRepository).save(any(Proposta.class));
    }

    @Test
    void salvar_SemPropostaExistente_DeveSalvarComStatusATIVA() {
        when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(profissionalRepository.findById(PROFISSIONAL_ID)).thenReturn(Optional.of(profissional));
        when(propostaRepository.findByPedidoIdAndProfissionalIdAndStatus(PEDIDO_ID, PROFISSIONAL_ID, StatusProposta.ATIVA))
                .thenReturn(Optional.empty());
        when(propostaRepository.save(any(Proposta.class))).thenAnswer(inv -> inv.getArgument(0));

        propostaService.salvarOuAtualizarProposta(PEDIDO_ID, PROFISSIONAL_ID, requestDTO);

        ArgumentCaptor<Proposta> captor = ArgumentCaptor.forClass(Proposta.class);
        verify(propostaRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusProposta.ATIVA);
    }

    @Test
    void salvar_SemPropostaExistente_DeveSalvarHorarios() {
        when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(profissionalRepository.findById(PROFISSIONAL_ID)).thenReturn(Optional.of(profissional));
        when(propostaRepository.findByPedidoIdAndProfissionalIdAndStatus(PEDIDO_ID, PROFISSIONAL_ID, StatusProposta.ATIVA))
                .thenReturn(Optional.empty());
        when(propostaRepository.save(any(Proposta.class))).thenAnswer(inv -> inv.getArgument(0));

        propostaService.salvarOuAtualizarProposta(PEDIDO_ID, PROFISSIONAL_ID, requestDTO);

        ArgumentCaptor<Proposta> captor = ArgumentCaptor.forClass(Proposta.class);
        verify(propostaRepository).save(captor.capture());
        assertThat(captor.getValue().getHorarios()).hasSize(2);
    }

    // --- Happy path: atualização ---

    @Test
    void salvar_ComPropostaAtiva_DeveAtualizarERetornarCriadaFalse() {
        Proposta existente = propostaExistente();
        when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(profissionalRepository.findById(PROFISSIONAL_ID)).thenReturn(Optional.of(profissional));
        when(propostaRepository.findByPedidoIdAndProfissionalIdAndStatus(PEDIDO_ID, PROFISSIONAL_ID, StatusProposta.ATIVA))
                .thenReturn(Optional.of(existente));
        when(propostaRepository.existsByPedidoIdAndStatus(PEDIDO_ID, StatusProposta.ACEITA)).thenReturn(false);
        when(propostaRepository.save(any(Proposta.class))).thenAnswer(inv -> inv.getArgument(0));

        PropostaService.PropostaSalvaResult resultado =
                propostaService.salvarOuAtualizarProposta(PEDIDO_ID, PROFISSIONAL_ID, requestDTO);

        assertThat(resultado.criada()).isFalse();
    }

    @Test
    void salvar_ComPropostaAtiva_DeveAtualizarValorEDescricao() {
        Proposta existente = propostaExistente();
        when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(profissionalRepository.findById(PROFISSIONAL_ID)).thenReturn(Optional.of(profissional));
        when(propostaRepository.findByPedidoIdAndProfissionalIdAndStatus(PEDIDO_ID, PROFISSIONAL_ID, StatusProposta.ATIVA))
                .thenReturn(Optional.of(existente));
        when(propostaRepository.existsByPedidoIdAndStatus(PEDIDO_ID, StatusProposta.ACEITA)).thenReturn(false);
        when(propostaRepository.save(any(Proposta.class))).thenAnswer(inv -> inv.getArgument(0));

        propostaService.salvarOuAtualizarProposta(PEDIDO_ID, PROFISSIONAL_ID, requestDTO);

        assertThat(existente.getValorOferecido()).isEqualByComparingTo(BigDecimal.valueOf(250.00));
        assertThat(existente.getDescricao()).isEqualTo("Serviço completo com garantia");
    }

    @Test
    void salvar_ComPropostaAtiva_DeveSubstituirHorarios() {
        Proposta existente = propostaExistente();
        when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(profissionalRepository.findById(PROFISSIONAL_ID)).thenReturn(Optional.of(profissional));
        when(propostaRepository.findByPedidoIdAndProfissionalIdAndStatus(PEDIDO_ID, PROFISSIONAL_ID, StatusProposta.ATIVA))
                .thenReturn(Optional.of(existente));
        when(propostaRepository.existsByPedidoIdAndStatus(PEDIDO_ID, StatusProposta.ACEITA)).thenReturn(false);
        when(propostaRepository.save(any(Proposta.class))).thenAnswer(inv -> inv.getArgument(0));

        propostaService.salvarOuAtualizarProposta(PEDIDO_ID, PROFISSIONAL_ID, requestDTO);

        assertThat(existente.getHorarios()).hasSize(2);
    }

    // --- Validação: status do pedido ---

    @Test
    void salvar_PedidoNaoAberto_DeveLancar422() {
        pedido.setStatus(StatusPedido.EM_ANDAMENTO);
        when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> propostaService.salvarOuAtualizarProposta(PEDIDO_ID, PROFISSIONAL_ID, requestDTO))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        verify(propostaRepository, never()).save(any());
    }

    @Test
    void salvar_PedidoNaoEncontrado_DeveLancar404() {
        when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> propostaService.salvarOuAtualizarProposta(PEDIDO_ID, PROFISSIONAL_ID, requestDTO))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // --- Validação: proposta própria ---

    @Test
    void salvar_ProfissionalEhDonoDoPedido_DeveLancar403() {
        cliente.setId(PROFISSIONAL_ID);
        when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> propostaService.salvarOuAtualizarProposta(PEDIDO_ID, PROFISSIONAL_ID, requestDTO))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        verify(propostaRepository, never()).save(any());
    }

    // --- Validação: edição bloqueada por proposta já aceita ---

    @Test
    void salvar_PropostaAtivaExisteEJaHaAceita_DeveLancar422() {
        when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(profissionalRepository.findById(PROFISSIONAL_ID)).thenReturn(Optional.of(profissional));
        when(propostaRepository.findByPedidoIdAndProfissionalIdAndStatus(PEDIDO_ID, PROFISSIONAL_ID, StatusProposta.ATIVA))
                .thenReturn(Optional.of(propostaExistente()));
        when(propostaRepository.existsByPedidoIdAndStatus(PEDIDO_ID, StatusProposta.ACEITA)).thenReturn(true);

        assertThatThrownBy(() -> propostaService.salvarOuAtualizarProposta(PEDIDO_ID, PROFISSIONAL_ID, requestDTO))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        verify(propostaRepository, never()).save(any());
    }

    // --- Validação: horários ---

    @Test
    void salvar_HorarioFimAnteriorAoInicio_DeveLancar400() {
        requestDTO.setHorariosDisponiveis(List.of(
                horario(LocalDateTime.now().plusDays(1).plusHours(2), LocalDateTime.now().plusDays(1))
        ));
        when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(profissionalRepository.findById(PROFISSIONAL_ID)).thenReturn(Optional.of(profissional));

        assertThatThrownBy(() -> propostaService.salvarOuAtualizarProposta(PEDIDO_ID, PROFISSIONAL_ID, requestDTO))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void salvar_HorarioFimIgualAoInicio_DeveLancar400() {
        LocalDateTime momento = LocalDateTime.now().plusDays(1);
        requestDTO.setHorariosDisponiveis(List.of(horario(momento, momento)));
        when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(profissionalRepository.findById(PROFISSIONAL_ID)).thenReturn(Optional.of(profissional));

        assertThatThrownBy(() -> propostaService.salvarOuAtualizarProposta(PEDIDO_ID, PROFISSIONAL_ID, requestDTO))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void salvar_HorariosSeOverpoem_DeveLancar400() {
        LocalDateTime base = LocalDateTime.now().plusDays(1);
        requestDTO.setHorariosDisponiveis(List.of(
                horario(base, base.plusHours(3)),
                horario(base.plusHours(2), base.plusHours(5))
        ));
        when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(profissionalRepository.findById(PROFISSIONAL_ID)).thenReturn(Optional.of(profissional));

        assertThatThrownBy(() -> propostaService.salvarOuAtualizarProposta(PEDIDO_ID, PROFISSIONAL_ID, requestDTO))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void salvar_HorariosContiguos_NaoDeveLancarExcecao() {
        LocalDateTime base = LocalDateTime.now().plusDays(1);
        requestDTO.setHorariosDisponiveis(List.of(
                horario(base, base.plusHours(2)),
                horario(base.plusHours(2), base.plusHours(4))
        ));
        when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(profissionalRepository.findById(PROFISSIONAL_ID)).thenReturn(Optional.of(profissional));
        when(propostaRepository.findByPedidoIdAndProfissionalIdAndStatus(PEDIDO_ID, PROFISSIONAL_ID, StatusProposta.ATIVA))
                .thenReturn(Optional.empty());
        when(propostaRepository.save(any(Proposta.class))).thenAnswer(inv -> inv.getArgument(0));

        PropostaService.PropostaSalvaResult resultado =
                propostaService.salvarOuAtualizarProposta(PEDIDO_ID, PROFISSIONAL_ID, requestDTO);

        assertThat(resultado.criada()).isTrue();
    }

    // --- Helpers ---

    private HorarioPropostaDTO horario(LocalDateTime inicio, LocalDateTime fim) {
        HorarioPropostaDTO h = new HorarioPropostaDTO();
        h.setInicio(inicio);
        h.setFim(fim);
        return h;
    }

    private Proposta propostaSalva() {
        Proposta p = new Proposta();
        p.setId(99L);
        p.setPedido(pedido);
        p.setProfissional(profissional);
        p.setStatus(StatusProposta.ATIVA);
        p.setValorOferecido(BigDecimal.valueOf(250.00));
        return p;
    }

    private Proposta propostaExistente() {
        Proposta p = new Proposta();
        p.setId(50L);
        p.setPedido(pedido);
        p.setProfissional(profissional);
        p.setStatus(StatusProposta.ATIVA);
        p.setValorOferecido(BigDecimal.valueOf(100.00));
        p.setHorarios(new ArrayList<>());
        return p;
    }
}
