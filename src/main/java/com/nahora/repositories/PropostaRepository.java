package com.nahora.repositories;

import com.nahora.model.Proposta;
import com.nahora.model.enums.StatusProposta;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PropostaRepository extends JpaRepository<Proposta, Long> {

    // Seus métodos (da sua branch):
    List<Proposta> findByPedidoIdAndStatus(Long pedidoId, StatusProposta status);
    List<Proposta> findByPedidoId(Long pedidoId);

    // Métodos vindos da develop:
    Optional<Proposta> findByPedidoIdAndProfissionalIdAndStatus(Long pedidoId, Long profissionalId, StatusProposta status);
    boolean existsByPedidoIdAndStatus(Long pedidoId, StatusProposta status);
}