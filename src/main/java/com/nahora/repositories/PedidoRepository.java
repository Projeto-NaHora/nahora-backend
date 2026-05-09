package com.nahora.repositories;

import com.nahora.model.Cliente;
import com.nahora.model.Pedido;
import com.nahora.model.enums.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface PedidoRepository extends JpaRepository<Pedido,Long> {
    long countByClienteAndStatusIn(Cliente cliente, Collection<StatusPedido> statusList);
}
