package com.nahora.model;

import com.nahora.model.enums.CategoriaServico;
import com.nahora.model.enums.StatusVerificacao;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

import java.util.List;

@Entity
@Table(name = "profissional")
@Getter @Setter
public class Profissional extends Usuario {

    private String bio;

    // Assumindo que CategoriaServico é um Enum
    @ElementCollection(targetClass = CategoriaServico.class)
    @CollectionTable(name = "profissional_categorias", joinColumns = @JoinColumn(name = "profissional_id"))
    @Enumerated(EnumType.STRING)
    private List<CategoriaServico> categoriasAtendidas;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point localizacao;

    @Column(name = "raio_atuacao")
    private Double raioAtuacao;

    @Column(name = "total_servicos_executados")
    private Integer totalServicosExecutados = 0;

    private Boolean disponivel = false;

    @Column(name = "documento_url")
    private String documentoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_verificacao")
    private StatusVerificacao statusVerificacao;

    @Column(name = "plano_plus")
    private Boolean planoPlus = false;

    // Assumindo lista de URLs para o portfolio
    @ElementCollection
    @CollectionTable(name = "profissional_portfolio", joinColumns = @JoinColumn(name = "profissional_id"))
    private List<String> portfolio;
}