package com.nahora.model;

import com.nahora.model.enums.CategoriaServico;
import com.nahora.model.enums.StatusPedido;
import com.nahora.model.enums.Urgencia;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedido")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    private CategoriaServico categoria;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @ElementCollection
    @CollectionTable(name = "pedido_fotos", joinColumns = @JoinColumn(name = "pedido_id"))
    private List<String> fotos = new ArrayList<>();

    @Embedded
    private Endereco endereco;

    @Enumerated(EnumType.STRING)
    private Urgencia urgencia;

    @Column(name = "orcamento_estimado")
    private BigDecimal orcamentoEstimado;

    @Enumerated(EnumType.STRING)
    private StatusPedido status;

    @CreatedDate
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}