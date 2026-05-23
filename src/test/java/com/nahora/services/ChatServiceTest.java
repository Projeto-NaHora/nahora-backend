package com.nahora.services;

import com.nahora.dto.request.MensagemRequestDTO;
import com.nahora.dto.response.MensagemResponseDTO;
import com.nahora.model.Conversa;
import com.nahora.model.Mensagem;
import com.nahora.model.Pedido;
import com.nahora.model.Proposta;
import com.nahora.model.Cliente;
import com.nahora.model.Profissional;
import com.nahora.model.enums.StatusConversa;
import com.nahora.model.enums.StatusProposta;
import com.nahora.repositories.ConversaRepository;
import com.nahora.repositories.MensagemRepository;
import com.nahora.repositories.PedidoRepository;
import com.nahora.repositories.PropostaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

    @Mock private ConversaRepository conversaRepository;
    @Mock private MensagemRepository mensagemRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private PushNotificationService pushNotificationService;
    @Mock private PedidoRepository pedidoRepository;
    @Mock private PropostaRepository propostaRepository;

    private Conversa conversaAtiva;
    private Cliente cliente;
    private Profissional profissional;

    @BeforeEach
    void setUp() {
        // Mock do Cliente
        cliente = mock(Cliente.class);
        lenient().when(cliente.getId()).thenReturn(1L);
        lenient().when(cliente.getNome()).thenReturn("Cliente João");
        lenient().when(cliente.getTokenFcm()).thenReturn("token-123");

        // Mock do Profissional
        profissional = mock(Profissional.class);
        lenient().when(profissional.getId()).thenReturn(2L);
        lenient().when(profissional.getTokenFcm()).thenReturn("token-do-profissional-456");

        // Mock das Entidades Relacionadas
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);

        Proposta proposta = new Proposta();
        proposta.setProfissional(profissional);
        proposta.setStatus(StatusProposta.ATIVA);

        // Mock da Conversa
        conversaAtiva = new Conversa();
        conversaAtiva.setId(100L);
        conversaAtiva.setPedido(pedido);
        conversaAtiva.setProposta(proposta);
        conversaAtiva.setStatus(StatusConversa.ABERTA);
    }

    @Test
    @DisplayName("Deve enviar mensagem limpa com sucesso")
    void deveEnviarMensagemComSucesso() {
        when(conversaRepository.findById(100L)).thenReturn(Optional.of(conversaAtiva));

        when(mensagemRepository.save(any(Mensagem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MensagemRequestDTO dto = new MensagemRequestDTO("Olá, qual o valor?", null);

        chatService.enviarMensagem(100L, 1L, dto);

        verify(mensagemRepository).save(any());
        // Verificando se o tipo exato está sendo enviado
        verify(messagingTemplate).convertAndSend(eq("/topic/conversa/100"), any(MensagemResponseDTO.class));
        // Verificando se a notificação foi para o profissional e não para o próprio cliente
        verify(pushNotificationService).enviarNotificacao(eq("token-do-profissional-456"), anyString(), anyString());
    }

    @Test
    @DisplayName("IA deve bloquear xingamentos independentemente do status da proposta")
    void iaDeveBloquearPalavrao() {
        when(conversaRepository.findById(100L)).thenReturn(Optional.of(conversaAtiva));
        when(mensagemRepository.save(any(Mensagem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MensagemRequestDTO dto = new MensagemRequestDTO("Seu golpe não vai funcionar", null);

        chatService.enviarMensagem(100L, 1L, dto);

        verify(mensagemRepository).save(argThat(msg -> msg.getBloqueadaIa().equals(true)));

        // Garante que absolutamente NENHUMA mensagem foi para o WebSocket
        verifyNoInteractions(messagingTemplate);
        // Garante que o usuário não recebeu notificação de um xingamento bloqueado
        verifyNoInteractions(pushNotificationService);
    }

    @Test
    @DisplayName("IA deve bloquear telefone antes de aceitar a proposta")
    void iaDeveBloquearContatoAntesDoAceite() {
        when(conversaRepository.findById(100L)).thenReturn(Optional.of(conversaAtiva));
        when(mensagemRepository.save(any(Mensagem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MensagemRequestDTO dto = new MensagemRequestDTO("Me chama no whats 99999-8888", null);

        chatService.enviarMensagem(100L, 1L, dto);

        verify(mensagemRepository, times(1)).save(argThat(msg -> msg.getBloqueadaIa().equals(true)));
    }

    @Test
    @DisplayName("IA deve PERMITIR telefone DEPOIS do aceite da proposta")
    void iaDevePermitirContatoDepoisDoAceite() {
        conversaAtiva.getProposta().setStatus(StatusProposta.ACEITA);
        when(conversaRepository.findById(100L)).thenReturn(Optional.of(conversaAtiva));
        when(mensagemRepository.save(any(Mensagem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MensagemRequestDTO dto = new MensagemRequestDTO("Meu numero é 99999-8888 para você interfonar", null);

        chatService.enviarMensagem(100L, 1L, dto);

        verify(mensagemRepository, times(1)).save(argThat(msg -> msg.getBloqueadaIa().equals(false)));
        verify(messagingTemplate, times(1)).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("Deve lançar erro 403 se um intruso tentar enviar mensagem")
    void deveLancarErroParaIntruso() {
        when(conversaRepository.findById(100L)).thenReturn(Optional.of(conversaAtiva));
        MensagemRequestDTO dto = new MensagemRequestDTO("Oi", null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            chatService.enviarMensagem(100L, 99L, dto); // ID 99 não é participante
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }


    // TESTES ADICIONAIS: FLUXOS DE CANAL (Abertura, Fechamento e Disputa)
    @Test
    @DisplayName("Deve abrir um canal com sucesso quando não há canal ativo para a proposta")
    void deveAbrirCanalComSucesso() {
        Long pedidoId = 100L;
        Long propostaId = 50L;

        Pedido pedido = new Pedido();
        Proposta proposta = new Proposta();

        when(conversaRepository.findByPropostaId(propostaId)).thenReturn(Optional.empty());
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(propostaRepository.findById(propostaId)).thenReturn(Optional.of(proposta));

        chatService.abrirCanal(pedidoId, propostaId);

        verify(conversaRepository, times(1)).save(argThat(conversa ->
                conversa.getPedido() == pedido &&
                        conversa.getProposta() == proposta &&
                        conversa.getStatus() == StatusConversa.ABERTA
        ));
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException ao tentar abrir canal para proposta que já possui chat")
    void deveLancarErroAoAbrirCanalDuplicado() {
        Long propostaId = 50L;
        when(conversaRepository.findByPropostaId(propostaId)).thenReturn(Optional.of(new Conversa()));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            chatService.abrirCanal(100L, propostaId);
        });

        assertTrue(exception.getMessage().contains("Conflito: Já existe um canal de chat"));
        verifyNoInteractions(pedidoRepository, propostaRepository);
    }

    @Test
    @DisplayName("Deve transicionar canais ativos para SOMENTE_LEITURA ao encerrar parcialmente")
    void deveEncerrarCanalParcialmente() {
        Long pedidoId = 100L;

        Conversa conversa1 = new Conversa();
        conversa1.setId(10L);
        conversa1.setStatus(StatusConversa.ABERTA);

        when(conversaRepository.findByPedidoIdAndStatusNot(pedidoId, StatusConversa.FECHADA))
                .thenReturn(List.of(conversa1));

        chatService.encerrarCanal(pedidoId);

        assertEquals(StatusConversa.SOMENTE_LEITURA, conversa1.getStatus());
        verify(conversaRepository, times(1)).save(conversa1);
    }

    @Test
    @DisplayName("Deve fechar definitivamente todos os canais não fechados de um pedido")
    void deveFecharCanalDefinitivamente() {
        Long pedidoId = 100L;
        Conversa conversa = new Conversa();
        conversa.setStatus(StatusConversa.SOMENTE_LEITURA);

        when(conversaRepository.findByPedidoIdAndStatusNot(pedidoId, StatusConversa.FECHADA))
                .thenReturn(List.of(conversa));

        chatService.fecharCanal(pedidoId);

        assertEquals(StatusConversa.FECHADA, conversa.getStatus());
        verify(conversaRepository, times(1)).save(conversa);
    }

    @Test
    @DisplayName("Deve fechar canais de propostas concorrentes mantendo apenas o da proposta aceita")
    void deveEncerrarCanaisPorPropostasRecusadas() {
        Long pedidoId = 100L;
        Long propostaAceitaId = 50L;

        Proposta pAceita = new Proposta(); pAceita.setId(propostaAceitaId);
        Proposta pRecusada = new Proposta(); pRecusada.setId(88L);

        Conversa cAtivaPropostaAceita = new Conversa();
        cAtivaPropostaAceita.setProposta(pAceita);
        cAtivaPropostaAceita.setStatus(StatusConversa.ABERTA);

        Conversa cAtivaPropostaConcorrente = new Conversa();
        cAtivaPropostaConcorrente.setProposta(pRecusada);
        cAtivaPropostaConcorrente.setStatus(StatusConversa.ABERTA);

        when(conversaRepository.findByPedidoIdAndStatusNot(pedidoId, StatusConversa.FECHADA))
                .thenReturn(List.of(cAtivaPropostaAceita, cAtivaPropostaConcorrente));

        chatService.encerrarCanaisPorPropostasRecusadas(pedidoId, propostaAceitaId);

        assertEquals(StatusConversa.ABERTA, cAtivaPropostaAceita.getStatus());
        assertEquals(StatusConversa.FECHADA, cAtivaPropostaConcorrente.getStatus());
        verify(conversaRepository, times(1)).save(cAtivaPropostaConcorrente);
    }

    @Test
    @DisplayName("Deve alterar status de SOMENTE_LEITURA para EM_DISPUTA ao reabrir canal")
    void deveReabrirCanalParaDisputa() {
        Long pedidoId = 100L;
        Conversa conversa = new Conversa();
        conversa.setStatus(StatusConversa.SOMENTE_LEITURA);

        when(conversaRepository.findByPedidoIdAndStatusNot(pedidoId, StatusConversa.FECHADA))
                .thenReturn(List.of(conversa));

        chatService.reabrirCanalParaDisputa(pedidoId);

        assertEquals(StatusConversa.EM_DISPUTA, conversa.getStatus());
        verify(conversaRepository, times(1)).save(conversa);
    }

    // TESTES DE HISTÓRICO E SEGURANÇA DE CONVERSA
    @Test
    @DisplayName("Deve retornar histórico paginado e marcar mensagens do outro participante como LIDA")
    void deveBuscarHistoricoEMarcarComoLida() {
        Long conversaId = 100L;
        Long usuarioLogadoId = 1L;

        Profissional remetenteProfissional = new Profissional();
        remetenteProfissional.setId(2L);
        remetenteProfissional.setNome("Profissional Fulano");

        Mensagem msgRecebida = new Mensagem();
        msgRecebida.setId(500L);
        msgRecebida.setConversa(conversaAtiva);
        msgRecebida.setRemetente(remetenteProfissional);
        msgRecebida.setConteudo("Boa tarde!");
        msgRecebida.setStatus(com.nahora.model.enums.StatusMensagem.ENVIADA);
        msgRecebida.setBloqueadaIa(false);

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<Mensagem> pagina = new org.springframework.data.domain.PageImpl<>(List.of(msgRecebida));

        when(conversaRepository.findById(conversaId)).thenReturn(Optional.of(conversaAtiva));
        when(mensagemRepository.findByConversaIdOrderByCriadoEmAsc(conversaId, pageable)).thenReturn(pagina);

        org.springframework.data.domain.Page<com.nahora.dto.response.MensagemResponseDTO> resultado =
                chatService.buscarHistorico(conversaId, usuarioLogadoId, pageable);

        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
        assertEquals("Boa tarde!", resultado.getContent().get(0).conteudo());
        assertEquals(com.nahora.model.enums.StatusMensagem.LIDA, msgRecebida.getStatus());
        verify(mensagemRepository, times(1)).save(msgRecebida);
    }

    @Test
    @DisplayName("Deve ocultar o conteúdo real se a mensagem foi bloqueada pela IA ao listar histórico")
    void deveOcultarConteudoSeMensagemBloqueadaIa() {
        Long conversaId = 100L;
        Long usuarioLogadoId = 1L;

        Mensagem msgBloqueada = new Mensagem();
        msgBloqueada.setId(600L);
        msgBloqueada.setConversa(conversaAtiva);
        msgBloqueada.setRemetente(cliente);
        msgBloqueada.setConteudo("Texto Ofensivo Comercial");
        msgBloqueada.setBloqueadaIa(true);

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<Mensagem> pagina = new org.springframework.data.domain.PageImpl<>(List.of(msgBloqueada));

        when(conversaRepository.findById(conversaId)).thenReturn(Optional.of(conversaAtiva));
        when(mensagemRepository.findByConversaIdOrderByCriadoEmAsc(conversaId, pageable)).thenReturn(pagina);

        org.springframework.data.domain.Page<com.nahora.dto.response.MensagemResponseDTO> resultado =
                chatService.buscarHistorico(conversaId, usuarioLogadoId, pageable);

        assertEquals("[Mensagem bloqueada por violar as diretrizes do sistema]", resultado.getContent().get(0).conteudo());
    }

    @Test
    @DisplayName("Deve lançar AccessDeniedException se um usuário externo tentar acessar o histórico")
    void deveBarrarHistoricoParaUsuarioNaoParticipante() {
        Long conversaId = 100L;
        Long usuarioInvasorId = 99L;

        when(conversaRepository.findById(conversaId)).thenReturn(Optional.of(conversaAtiva));
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            chatService.buscarHistorico(conversaId, usuarioInvasorId, pageable);
        });
    }
}