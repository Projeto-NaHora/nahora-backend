package com.nahora.repositories.specification;

import com.nahora.dto.request.PedidoDistanceRequest;
import com.nahora.model.Pedido;
import com.nahora.model.enums.Urgencia;
import com.nahora.repositories.PedidoRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class PedidoRepositoryImpl implements PedidoRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<PedidoDistanceRequest> findWithFiltersAndDistance(
            Point profissionalLocation,
            Double raioKm,
            Specification<Pedido> specification,
            Pageable pageable) {

        CriteriaBuilder cb = em.getCriteriaBuilder();


        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<Pedido> root = cq.from(Pedido.class);
        Path<Object> coordenadas = root.get("endereco").get("coordenadas");

        Expression<Double> distanciaEmMetros = cb.function(
                "ST_DistanceSphere", Double.class,
                coordenadas,
                cb.function("ST_SetSRID", Point.class, cb.literal(profissionalLocation), cb.literal(4326))
        );

        Expression<Double> distanciaKm = cb.prod(distanciaEmMetros, cb.literal(0.001));

        cq.multiselect(root, distanciaKm.alias("distancia_km"));

        Predicate filtrosDinamicos = specification.toPredicate(root, cq, cb);
        Predicate dentroDoRaio = cb.lessThanOrEqualTo(distanciaEmMetros, raioKm * 1000);

        Predicate finalPredicate = filtrosDinamicos != null
                ? cb.and(filtrosDinamicos, dentroDoRaio)
                : dentroDoRaio;

        cq.where(finalPredicate);

        List<Order> orders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            String property = order.getProperty();
            if (property.equals("distanciaKm")) {
                orders.add(order.isAscending() ? cb.asc(distanciaKm) : cb.desc(distanciaKm));
            }
            else if (property.equals("urgente")) {
                Expression<?> urgenteExpr = cb.selectCase()
                        .when(cb.equal(root.get("urgencia"), Urgencia.URGENTE), cb.literal(1))
                        .otherwise(cb.literal(0));
                orders.add(cb.desc(urgenteExpr));
            }
            else {
                Path<Object> path = root.get(property);
                orders.add(order.isAscending() ? cb.asc(path) : cb.desc(path));
            }
        }
        cq.orderBy(orders);

        TypedQuery<Object[]> query = em.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        List<Object[]> resultList = query.getResultList();

        List<PedidoDistanceRequest> dtos = resultList.stream()
                .map(row -> new PedidoDistanceRequest((Pedido) row[0], (Double) row[1]))
                .toList();

        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<Pedido> countRoot = countCq.from(Pedido.class);
        Path<Object> countCoordenadas = countRoot.get("endereco").get("coordenadas");

        Expression<Double> countDistanciaEmMetros = cb.function(
                "ST_DistanceSphere", Double.class,
                countCoordenadas,
                cb.function("ST_SetSRID", Point.class, cb.literal(profissionalLocation), cb.literal(4326))
        );

        Predicate countFiltrosDinamicos = specification.toPredicate(countRoot, countCq, cb);
        Predicate countDentroRaio = cb.lessThanOrEqualTo(countDistanciaEmMetros, raioKm * 1000);

        Predicate countFinalPredicate = countFiltrosDinamicos != null
                ? cb.and(countFiltrosDinamicos, countDentroRaio)
                : countDentroRaio;

        countCq.where(countFinalPredicate);
        countCq.select(cb.count(countRoot));

        Long total = em.createQuery(countCq).getSingleResult();

        return new PageImpl<>(dtos, pageable, total);
    }
}