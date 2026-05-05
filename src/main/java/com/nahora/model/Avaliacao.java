package com.nahora.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "avaliacao",
        uniqueConstraints = @UniqueConstraint(columnNames = {"pedido_id", "avaliador_id"})
)
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @ManyToOne(optional = false)
    @JoinColumn(name = "avaliador_id")
    private Usuario avaliador;

    @ManyToOne(optional = false)
    @JoinColumn(name = "avaliado_id")
    private Usuario avaliado;

    @Column(name = "nota_geral", nullable = false)
    private Integer notaGeral;

    private Integer pontualidade;
    private Integer qualidade;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    @CreatedDate
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}