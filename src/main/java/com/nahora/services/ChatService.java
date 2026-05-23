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

    private final ConversaRepository conversaRepository;
    private final MensagemRepository mensagemRepository;

    private final SimpMessagingTemplate messagingTemplate;
    
    
    public void abrirCanal(Long pedidoId, Long propostaId) {
    }

    public void encerrarCanal(Long pedidoId) {
    }

    public void fecharCanal(Long pedidoId) {
    }

    public void encerrarCanaisPorPropostasRecusadas(Long pedidoId, Long propostaAceitaId) {
    }

    public void reabrirCanalParaDisputa(Long pedidoId) {
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
        remetenteRef.setId(remetenteId);
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
                "Nome do Remetente", // Pode buscar o nome se necessário
                mensagem.getConteudo(),
                mensagem.getAnexoUrl(),
                mensagem.getStatus(),
                mensagem.getBloqueadaIa(),
                mensagem.getCriadoEm()
        );
        
        messagingTemplate.convertAndSend("/topic/conversa/" + conversaId, responseDTO);

        // Notificação Push (Mock)
        // pushNotificationService.enviarNotificacaoOffline(destinatarioId, "Nova mensagem de " + nomeRemetente);
        log.info("Mensagem {} enviada no canal /topic/conversa/{}", mensagem.getId(), conversaId);
    }
}