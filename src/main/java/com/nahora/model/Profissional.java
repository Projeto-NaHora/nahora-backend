package com.nahora.model;

import com.nahora.model.enums.CategoriaServico;
import com.nahora.model.enums.StatusVerificacao;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.validator.constraints.br.CPF;
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

    @ElementCollection
    @CollectionTable(name = "profissional_especialidades", joinColumns = @JoinColumn(name = "profissional_id"))
    @Column(name = "especialidade")
    private List<String> especialidades;

    @Column(name = "total_servicos_executados")
    private Integer totalServicosExecutados = 0;

    private Boolean disponivel = false;

    @Column(name = "rg_frente_url")
    private String rgFrenteUrl;

    @Column(name = "rg_verso_url")
    private String rgVersoUrl;

    @Column(name = "selfie_url")
    private String selfieUrl;

    @Column(name = "anos_experiencia")
    private Integer anosExperiencia;

    @CPF
    @Column(unique = true)
    private String cpf;

    @ElementCollection
    @CollectionTable(
            name = "profissional_areas_atuacao",
            joinColumns = @JoinColumn(name = "profissional_id")
    )
    @Column(name = "bairro_ou_area")
    private List<String> areaAtuacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_verificacao")
    private StatusVerificacao statusVerificacao;

    @Column(name = "perfil_completo")
    private Boolean perfilCompleto = false;

    @Column(name = "plano_plus")
    private Boolean planoPlus = false;

    // Assumindo lista de URLs para o portfolio
    @ElementCollection
    @CollectionTable(name = "profissional_portfolio", joinColumns = @JoinColumn(name = "profissional_id"))
    private List<String> portfolio;

    @Column(name = "nota_media")
    private Double notaMedia = 5.0; // Valor padrão inicial

    @Column(name = "numero_avaliacoes")
    private Integer numeroAvaliacoes = 0;
}