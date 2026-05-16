package com.nahora.repositories.specification;

import com.nahora.model.Pedido;
import com.nahora.model.enums.CategoriaServico;
import com.nahora.model.enums.StatusPedido;
import com.nahora.model.enums.Urgencia;
import org.springframework.data.jpa.domain.Specification;

public class PedidoSpecifications {

    public static Specification<Pedido> hasCategoria(CategoriaServico categoria) {
        return (root, query, cb) -> {
            if (categoria == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("categoria"), categoria);
        };
    }

    public static Specification<Pedido> isUrgente(Boolean urgente) {
        return (root, query, cb) -> {
            if (urgente == null) {
                return cb.conjunction();
            }
            if (urgente) {
                return cb.equal(root.get("urgencia"), Urgencia.URGENTE);
            } else {
                return cb.notEqual(root.get("urgencia"), Urgencia.URGENTE);
            }
        };
    }
    public static Specification<Pedido> isAberto() {
        return (root, query, cb) -> cb.equal(root.get("status"), StatusPedido.ABERTO);
    }
}