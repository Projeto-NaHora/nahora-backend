package com.nahora.dto.request;

import com.nahora.model.enums.CategoriaServico;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PedidoFiltroRequest {

    private CategoriaServico categoria;

    private Boolean urgente;

    private SortBy sortBy;

    public enum SortBy {
        MAIS_PROXIMOS,
        MAIS_RECENTES,
        URGENTES
    }
}
