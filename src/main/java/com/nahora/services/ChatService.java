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

    private final PropostaRepository propostaRepository;

    private final SimpMessagingTemplate messagingTemplate;

    // Abertura de canal: Cria uma conversa ABERTA vinculada à proposta. Lança conflito se a proposta já possuir um canal ativo.
    @org.springframework.transaction.annotation.Transactional
    public void abrirCanal(Long propostaId) {
        log.info("[CHAT] Tentando abrir canal para a proposta {}", propostaId);

        // Valida se já existe conversa para esta proposta
        conversaRepository.findByPropostaId(propostaId).ifPresent(c -> {
            throw new IllegalStateException("Conflito: Já existe um canal de chat aberto para essa proposta");
        });

        var proposta = propostaRepository.findById(propostaId)
                .orElseThrow(() -> new IllegalStateException("Proposta não encontrada"));

        // Cria a nova conversa
        var novaConversa = new com.nahora.model.Conversa();
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
        conversaRepository.findByPropostaPedidoIdAndStatusNot(pedidoId, StatusConversa.FECHADA).stream()
                .filter(c->c.getStatus() == StatusConversa.ABERTA)
                .forEach(conversa -> {
                    conversa.setStatus(StatusConversa.SOMENTE_LEITURA);
                    conversaRepository.save(conversa);
                    log.info("[CHAT] Conversa {} alterada para SOMENTE_LEITURA", conversa.getId());
                });
    }
    // Encerramento definitivo: Transiciona o canal para FECHADA. Chamado quando o pedido transiciona para COMPLETED (cliente confirma conclusão do serviço).
    @org.springframework.transaction.annotation.Transactional
    public void fecharCanal(Long pedidoId) {
        log.info("[CHAT] Fechando definitivamente os canais do pedido {}", pedidoId);

        conversaRepository.findByPropostaPedidoIdAndStatusNot(pedidoId, StatusConversa.FECHADA).stream()
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

        List<com.nahora.model.Conversa> conversas = conversaRepository.findByPropostaPedidoIdAndStatusNot(pedidoId, StatusConversa.FECHADA);

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

        conversaRepository.findByPropostaPedidoIdAndStatusNot(pedidoId, StatusConversa.FECHADA).stream()
                .filter(c -> c.getStatus() == StatusConversa.SOMENTE_LEITURA)
                .forEach(conversa -> {
                    conversa.setStatus(StatusConversa.EM_DISPUTA);
                    conversaRepository.save(conversa);
                    log.info("[CHAT] Conversa {} colocada EM_DISPUTA", conversa.getId());
                });
    }

    // Histórico de Mensagens: Retorna mensagens paginadas e ordenadas por data ASC.
    // Filtra ocultando as que foram bloqueadas pela IA.
    // Regra: Ao buscar o histórico, atualiza as mensagens recebidas pelo usuário de ENTREGUE para LIDA.
    @org.springframework.transaction.annotation.Transactional // 🌟 ADICIONADO: Garante que o update do status funcione no banco
    public Page<MensagemResponseDTO> buscarHistorico(Long conversaId, Long usuarioId, Pageable pageable) {
        log.info("[CHAT] Buscando histórico para a conversa {} solicitada pelo usuário {}", conversaId, usuarioId);

        // Valida se a conversa existe e se o usuário faz parte dela
        var conversa = conversaRepository.findById(conversaId)
                .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada")); // Corrigido digitação "encotrada"

        boolean ehCliente = conversa.getProposta().getPedido().getCliente().getId().equals(usuarioId);
        boolean ehProfissional = conversa.getProposta().getProfissional().getId().equals(usuarioId);

        if(!ehCliente && !ehProfissional){
            throw new org.springframework.security.access.AccessDeniedException("Acesso negado: Você não participa desta conversa.");
        }

        // Busca as mensagens paginadas (A query do repository já ordena por criadoEm Asc)
        Page<com.nahora.model.Mensagem> paginaMensagens = mensagemRepository.findByConversaIdOrderByCriadoEmAsc(conversaId, pageable);

        // Atualiza o status das mensagens que foram enviadas pelo OUTRO participante para LIDA
        paginaMensagens.forEach(mensagem -> {
            // Se a mensagem não foi enviada pelo usuário logado, e o status não for LIDA, ela passa a ser LIDA
            if(!mensagem.getRemetente().getId().equals(usuarioId) && mensagem.getStatus() != StatusMensagem.LIDA){
                mensagem.setStatus(StatusMensagem.LIDA);
                mensagemRepository.save(mensagem);
            }
        });

        // Converte e retorna as mensagens aplicando a regra de negócio da IA
        return paginaMensagens
                .map(msg -> {
                    // Se foi bloqueada pela IA, omitimos o conteúdo real por segurança no front-end
                    String conteudoExibicao = Boolean.TRUE.equals(msg.getBloqueadaIa())
                            ? "[Mensagem bloqueada por violar as diretrizes do sistema]"
                            : msg.getConteudo();

                    return new MensagemResponseDTO(
                            msg.getId(),
                            msg.getConversa().getId(),
                            msg.getRemetente().getId(),
                            msg.getRemetente().getNome(),
                            conteudoExibicao,
                            msg.getAnexoUrl(),
                            msg.getStatus(),
                            msg.getBloqueadaIa(),
                            msg.getCriadoEm()
                    );
                });
    }

    // Listagem de Conversas: Lista os canais de chat vinculados ao usuário logado, injetando os dados resumidos do pedido e os dados do outro participante de forma inteligente.
    @org.springframework.transaction.annotation.Transactional(readOnly = true) // Boa prática: readOnly para consultas
    public Page<ConversaResponseDTO> listarConversas(Long usuarioId, List<StatusConversa> filtro, Pageable pageable) {
        log.info("[CHAT] Listando conversas para o usuário {} com filtros de status {}", usuarioId, filtro);

        // Busca as conversas onde o usuário participa utilizando a Query Customizada do repositório
        Page<com.nahora.model.Conversa> conversas = conversaRepository.findAllByParticipanteIdAndStatus(usuarioId, filtro, pageable);

        return conversas.map(conversa -> {
            var pedido = conversa.getProposta().getPedido();

            // Identifica quem é o outro participante da conversa
            String nomeOutro = "";
            String fotoOutro = null;

            if (pedido.getCliente().getId().equals(usuarioId)) {
                // Se o usuário logado é o Cliente, o outro é o Profissional
                nomeOutro = conversa.getProposta().getProfissional().getNome();
                fotoOutro = conversa.getProposta().getProfissional().getFoto();
            } else {
                // Se o usuário logado é o Profissional, o outro é o Cliente
                nomeOutro = pedido.getCliente().getNome();
                fotoOutro = pedido.getCliente().getFoto();
            }

            return new ConversaResponseDTO(
                    conversa.getId(),
                    pedido.getId(),
                    conversa.getProposta().getId(),
                    conversa.getStatus(),
                    conversa.getCriadoEm(),
                    pedido.getDescricao(),
                    pedido.getCategoria().name(),
                    nomeOutro,
                    fotoOutro
            );
        });
    }

    @org.springframework.transaction.annotation.Transactional
    public void enviarMensagem(Long conversaId, Long remetenteId, MensagemRequestDTO dto) {

        Conversa conversa = conversaRepository.findById(conversaId)
                .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada"));

        // Validar se o remetente é participante
        Long clienteId = conversa.getProposta().getPedido().getCliente().getId();
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
                       conversa.getProposta().getPedido().getCliente() :
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
                                  conversa.getProposta().getPedido().getCliente();

        String tokenDestinatario = destinatarioRef.getTokenFcm();
        String tituloPush = "Nova mensagem de " + remetenteRef.getNome();

        pushNotificationService.enviarNotificacao(tokenDestinatario, tituloPush, dto.conteudo());

        log.info("Mensagem {} enviada no canal /topic/conversa/{}", mensagem.getId(), conversaId);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ConversaResponseDTO buscarConversaDaProposta(Long propostaId, Long usuarioId) {
        var conversa = conversaRepository.findByPropostaId(propostaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Nenhuma conversa encontrada para a proposta informada."));

        var pedido = conversa.getProposta().getPedido();
        boolean ehCliente = pedido.getCliente().getId().equals(usuarioId);
        boolean ehProfissional = conversa.getProposta().getProfissional().getId().equals(usuarioId);

        if (!ehCliente && !ehProfissional) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Voce nao e participante desta conversa.");
        }

        String nomeOutro;
        String fotoOutro;
        if (ehCliente) {
            nomeOutro = conversa.getProposta().getProfissional().getNome();
            fotoOutro = conversa.getProposta().getProfissional().getFoto();
        } else {
            nomeOutro = pedido.getCliente().getNome();
            fotoOutro = pedido.getCliente().getFoto();
        }

        return new ConversaResponseDTO(
                conversa.getId(),
                pedido.getId(),
                conversa.getProposta().getId(),
                conversa.getStatus(),
                conversa.getCriadoEm(),
                pedido.getDescricao(),
                pedido.getCategoria().name(),
                nomeOutro,
                fotoOutro
        );
    }
}