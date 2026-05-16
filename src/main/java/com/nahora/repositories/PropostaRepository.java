package com.nahora.repositories;

import com.nahora.model.Proposta;
import com.nahora.model.enums.StatusProposta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropostaRepository extends JpaRepository<Proposta, Long> {
    List<Proposta> findByPedidoIdAndStatus(Long pedidoId, StatusProposta status);

    List<Proposta> findByPedidoId(Long pedidoId);
}