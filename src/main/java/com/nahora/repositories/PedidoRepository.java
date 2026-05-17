package com.nahora.repositories;

import com.nahora.model.Cliente;
import com.nahora.model.Pedido;
import com.nahora.model.enums.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;

public interface PedidoRepository extends
        JpaRepository<Pedido, Long>,
        JpaSpecificationExecutor<Pedido>,
        PedidoRepositoryCustom {

    long countByClienteAndStatusIn(Cliente cliente, Collection<StatusPedido> statusList);
}