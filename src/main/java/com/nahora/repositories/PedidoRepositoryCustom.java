package com.nahora.repositories;

import com.nahora.dto.request.PedidoDistanceRequest;
import com.nahora.model.Pedido;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface PedidoRepositoryCustom {
    Page<PedidoDistanceRequest> findWithFiltersAndDistance(
            Point profissionalLocation,
            Double raioKm,
            Specification<Pedido> specification,
            Pageable pageable
    );
}
