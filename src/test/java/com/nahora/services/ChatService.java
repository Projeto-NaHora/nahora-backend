package com.nahora.services;

import com.nahora.dto.request.MensagemRequestDTO;
import com.nahora.dto.response.ConversaResponseDTO;
import com.nahora.dto.response.MensagemResponseDTO;
import com.nahora.model.enums.StatusConversa;
import com.nahora.repositories.ConversaRepository;
import com.nahora.repositories.MensagemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversaRepository conversaRepository;
    private final MensagemRepository mensagemRepository;
    
    
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
    
    }
}