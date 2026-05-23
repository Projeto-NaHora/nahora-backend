package com.nahora.services;

import com.nahora.dto.request.MensagemRequestDTO;
import com.nahora.dto.response.ConversaResponseDTO;
import com.nahora.dto.response.MensagemResponseDTO;
import com.nahora.model.Conversa;
import com.nahora.model.Mensagem;
import com.nahora.model.Usuario;
import com.nahora.model.enums.StatusConversa;
import com.nahora.model.enums.StatusMensagem;
import com.nahora.repositories.ConversaRepository;
import com.nahora.repositories.MensagemRepository;
import com.nahora.repositories.PedidoRepository;
import com.nahora.repositories.PropostaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.http.HttpStatus;


import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final PushNotificationService pushNotificationService;

    private final ConversaRepository conversaRepository;
    private final MensagemRepository mensagemRepository;

    private final PedidoRepository pedidoRepository;
    private final PropostaRepository propostaRepository;

    private final SimpMessagingTemplate messagingTemplate;

    // Abertura de canal: Cria uma conversa ABERTA vinculada ao pedido e à proposta. Lança conflito se a proposta já possuir um canal ativo.
    @org.springframework.transaction.annotation.Transactional
    public void abrirCanal(Long pedidoId, Long propostaId) {
        log.info("[CHAT] Tentando abrir canal para o pedido {} e proposta {}", pedidoId, propostaId);

        // Valida se já existe conversa para esta proposta
        conversaRepository.findByPropostaId(propostaId).ifPresent(c -> {
            throw new IllegalStateException("Conflito: Já existe um canal de chat aberto para essa proposta");
        });

        // Busca as entidades necessárias
        var pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalStateException("Pedido não encontrado"));
        var proposta = propostaRepository.findById(propostaId)
                .orElseThrow(() -> new IllegalStateException("Proposta não encontrada"));

        // Cria a nova conversa
        var novaConversa = new com.nahora.model.Conversa();
        novaConversa.setPedido(pedido);
        novaConversa.setProposta(proposta);
        novaConversa.setStatus(StatusConversa.ABERTA);

        conversaRepository.save(novaConversa);
        log.info("[CHAT] Canal criado com sucesso para a proposta {}", propostaId);

    }

    // Encerramento parcial: Transiciona o canal para SOMENTE_LEITURA. Chamado quando o profissional conclui o serviço (Aguardando Validação).
    @org.springframework.transaction.annotation.Transactional
    public void encerrarCanal(Long pedidoId) {
        log.info("[CHAT] Encerrando parcialmente canais ativos para SOMENTE_LEITURA do pedido {}", pedidoId);

        // Busca a conversa ativa (ABERTA) associada ao pedido que foi aceita
        conversaRepository.findByPedidoIdAndStatusNot(pedidoId, StatusConversa.FECHADA).stream()
                .filter(c->c.getStatus() == StatusConversa.ABERTA)
                .forEach(conversa -> {
                    conversa.setStatus(StatusConversa.SOMENTE_LEITURA);
                    conversaRepository.save(conversa);
                    log.info("[CHAT] Conversa {} alterada para SOMENTE_LEITURA", conversa.getId());
                });
    }
    // Encerramento definitivo: Transiciona o canal de SOMENTE_LEITURA para FECHADA. Chamado após a confirmação do pagamento pelo cliente.
    @org.springframework.transaction.annotation.Transactional
    public void fecharCanal(Long pedidoId) {
        log.info("[CHAT] Fechando definitivamente os canais do pedido {}", pedidoId);

        conversaRepository.findByPedidoIdAndStatusNot(pedidoId, StatusConversa.FECHADA).stream()
                .forEach(conversa -> {
                    conversa.setStatus(StatusConversa.FECHADA);
                    conversaRepository.save(conversa);
                    log.info("[CHAT] Conversa {} encerrada definitivamente (FECHADA)", conversa.getId());
                });
    }

    // Encerramento de concorrentes: Transiciona para FECHADA todos os canais das propostas recusadas. Chamado no momento do aceite da proposta principal.
    @org.springframework.transaction.annotation.Transactional
    public void encerrarCanaisPorPropostasRecusadas(Long pedidoId, Long propostaAceitaId) {
        log.info("[CHAT] Fechando canais concorrentes do pedido {}, exceto a proposta aceita {}", pedidoId, propostaAceitaId);

        List<com.nahora.model.Conversa> conversas = conversaRepository.findByPedidoIdAndStatusNot(pedidoId, StatusConversa.FECHADA);

        conversas.stream()
                .filter(c -> !c.getProposta().getId().equals(propostaAceitaId))
                .forEach(conversa -> {
                    conversa.setStatus(StatusConversa.FECHADA);
                    conversaRepository.save(conversa);
                    log.info("[CHAT] Canal concorrente {} fechado devido ao aceite de outra proposta", conversa.getId());
                });
    }

    // Disputa: Transiciona de SOMENTE_LEITURA para EM_DISPUTA, reabilitando o envio de mensagens.
    @org.springframework.transaction.annotation.Transactional
    public void reabrirCanalParaDisputa(Long pedidoId) {
        log.info("[CHAT] Reabrindo canal para disputa no pedido {}", pedidoId);

        conversaRepository.findByPedidoIdAndStatusNot(pedidoId, StatusConversa.FECHADA).stream()
                .filter(c -> c.getStatus() == StatusConversa.SOMENTE_LEITURA)
                .forEach(conversa -> {
                    conversa.setStatus(StatusConversa.EM_DISPUTA);
                    conversaRepository.save(conversa);
                    log.info("[CHAT] Conversa {} colocada EM_DISPUTA", conversa.getId());
                });
    }

    public Page<MensagemResponseDTO> buscarHistorico(Long conversaId, Long usuarioId, Pageable pageable) {
        return Page.empty();
    }

    public Page<ConversaResponseDTO> listarConversas(Long usuarioId, List<StatusConversa> filtro, Pageable pageable) {
        return Page.empty();
    }
  

    public void enviarMensagem(Long conversaId, Long remetenteId, MensagemRequestDTO dto) {

        Conversa conversa = conversaRepository.findById(conversaId)
                .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada"));

        // Validar se o remetente é participante
        Long clienteId = conversa.getPedido().getCliente().getId();
        Long profissionalId = conversa.getProposta().getProfissional().getId();
        
        if (!remetenteId.equals(clienteId) && !remetenteId.equals(profissionalId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não é participante desta conversa.");
        }

        // Validar Status da Conversa
        if (conversa.getStatus() != StatusConversa.ABERTA && conversa.getStatus() != StatusConversa.EM_DISPUTA) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Conversa encerrada ou em modo leitura.");
        }

        Mensagem mensagem = new Mensagem();
        mensagem.setConversa(conversa);
        Usuario remetenteRef = remetenteId.equals(clienteId) ? 
                       conversa.getPedido().getCliente() : 
                       conversa.getProposta().getProfissional(); 
        mensagem.setRemetente(remetenteRef);
        mensagem.setConteudo(dto.conteudo());
        mensagem.setAnexoUrl(dto.anexoUrl());
        mensagem.setStatus(StatusMensagem.ENVIADA);
        mensagem.setBloqueadaIa(false);

        // O Filtro de IA (Simulação)
        boolean isPropostaAceita = conversa.getProposta().getStatus().name().equals("ACEITA"); 
        
        // Simulação de IA verificando termos abusivos (sempre roda)
        if (dto.conteudo().toLowerCase().matches(".*(ofensa|palavrão|golpe).*")) {
            mensagem.setBloqueadaIa(true);
            mensagem.setMotivoBloqueio("Conteúdo abusivo detectado.");
        }
        
        // Simulação de IA verificando dados de contato (roda APENAS ANTES do fechamento da proposta)
        if (!isPropostaAceita && !mensagem.getBloqueadaIa()) {
            String regexContato = ".*(\\d{4,}-\\d{4,}|@|[wW]hats|[iI]nsta).*";
            if (dto.conteudo().matches(regexContato)) {
                mensagem.setBloqueadaIa(true);
                mensagem.setMotivoBloqueio("Tentativa de compartilhamento de contato antes do fechamento.");
            }
        }

        // Persiste a mensagem no banco
        mensagem = mensagemRepository.save(mensagem);

        // Roteamento: Se a mensagem foi bloqueada pela IA, ela é salva no banco, mas não é enviada
        if (mensagem.getBloqueadaIa()) {
            log.warn("Mensagem {} bloqueada na conversa {}. Motivo: {}", mensagem.getId(), conversaId, mensagem.getMotivoBloqueio());
            return; 
        }

        // Broadcast via WebSocket para o tópico da conversa
        MensagemResponseDTO responseDTO = new MensagemResponseDTO(
                mensagem.getId(),
                conversaId,
                remetenteId,
                remetenteRef.getNome(),
                mensagem.getConteudo(),
                mensagem.getAnexoUrl(),
                mensagem.getStatus(),
                mensagem.getBloqueadaIa(),
                mensagem.getCriadoEm()
        );
        
        messagingTemplate.convertAndSend("/topic/conversa/" + conversaId, responseDTO);

        Usuario destinatarioRef = remetenteId.equals(clienteId) ? 
                                  conversa.getProposta().getProfissional() : 
                                  conversa.getPedido().getCliente();

        String tokenDestinatario = destinatarioRef.getTokenFcm();
        String tituloPush = "Nova mensagem de " + remetenteRef.getNome();

        pushNotificationService.enviarNotificacao(tokenDestinatario, tituloPush, dto.conteudo());

        log.info("Mensagem {} enviada no canal /topic/conversa/{}", mensagem.getId(), conversaId);
    }
}