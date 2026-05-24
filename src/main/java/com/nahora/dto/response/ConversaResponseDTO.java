package com.nahora.dto.response;

import java.time.LocalDateTime;

import com.nahora.model.enums.StatusConversa;

public record ConversaResponseDTO(

    Long id,
    Long pedidoId,
    Long propostaId,
    StatusConversa status,
    LocalDateTime criadoEm,

    // Dados adicionais
    String tituloPedido,
    String categoriaPedido,
    String nomeOutroParticipante,
    String fotoOutroParticipante

) {}
