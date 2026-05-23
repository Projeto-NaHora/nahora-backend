package com.nahora.dto.response;

import java.time.LocalDateTime;

import com.nahora.model.enums.StatusMensagem;

public record MensagemResponseDTO(

    Long id,
    Long conversaId,
    Long remetenteId,
    String nomeRemetente,
    String conteudo,
    String anexoUrl,
    StatusMensagem status,
    Boolean bloqueadaIa,
    LocalDateTime criadoEm
    
) {}
