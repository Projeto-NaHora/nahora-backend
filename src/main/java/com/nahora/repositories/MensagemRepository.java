package com.nahora.repositories;

import com.nahora.model.Mensagem;
import com.nahora.model.enums.StatusMensagem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensagemRepository extends JpaRepository<Mensagem, Long> {

    // Histórico de mensagens da conversa, paginado
    Page<Mensagem> findByConversaIdOrderByCriadoEmAsc(Long conversaId, Pageable pageable);

    // Usado para encontrar mensagens que ainda não foram lidas para atualizar o status
    List<Mensagem> findByConversaIdAndStatusNot(Long conversaId, StatusMensagem status);
}