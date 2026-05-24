package com.nahora.model;

import com.nahora.model.enums.CategoriaServico;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "categoria")
@Getter @Setter @NoArgsConstructor
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String icone;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria_servico", nullable = false, unique = true)
    private CategoriaServico categoriaServico;

    @Column(name = "valor_sugerido_min")
    private BigDecimal valorSugeridoMin;

    @Column(name = "valor_sugerido_max")
    private BigDecimal valorSugeridoMax;
}
