package com.nahora.repositories;

import com.nahora.model.Conversa;
import com.nahora.model.enums.StatusConversa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversaRepository extends JpaRepository<Conversa, Long> {

    // Regra: Uma proposta gera apenas UM canal de conversa
    Optional<Conversa> findByPropostaId(Long propostaId);

    // Usado para fechar os canais dos concorrentes quando uma proposta é aceita
    @Query("SELECT c FROM Conversa c WHERE c.proposta.pedido.id = :pedidoId AND c.status <> :status")
    List<Conversa> findByPropostaPedidoIdAndStatusNot(@Param("pedidoId") Long pedidoId,
                                                       @Param("status") StatusConversa status);

    @Query("SELECT c FROM Conversa c WHERE " +
           "(c.proposta.pedido.cliente.id = :usuarioId OR c.proposta.profissional.id = :usuarioId) " +
           "AND c.status IN :statuses")
    Page<Conversa> findAllByParticipanteIdAndStatus(
            @Param("usuarioId") Long usuarioId,
            @Param("statuses") List<StatusConversa> statuses,
            Pageable pageable
    );
}