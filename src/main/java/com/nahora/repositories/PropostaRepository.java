package com.nahora.repositories;

import com.nahora.model.Proposta;
import com.nahora.model.enums.StatusProposta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropostaRepository extends JpaRepository<Proposta, Long> {

    Optional<Proposta> findByPedidoIdAndProfissionalIdAndStatus(Long pedidoId, Long profissionalId, StatusProposta status);

    boolean existsByPedidoIdAndStatus(Long pedidoId, StatusProposta status);

    int countByPedidoId(Long pedidoId);
}
