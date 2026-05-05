package com.nahora.model;

import com.nahora.model.enums.StatusDisputa;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "disputa")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public class Disputa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "pedido_id", unique = true)
    private Pedido pedido;

    @ManyToOne(optional = false)
    @JoinColumn(name = "aberta_por_id")
    private Usuario abertaPor;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String motivo;

    @ElementCollection
    @CollectionTable(name = "disputa_evidencias", joinColumns = @JoinColumn(name = "disputa_id"))
    private List<String> evidencias = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusDisputa status;

    @ManyToOne
    @JoinColumn(name = "resolvida_por_id")
    private Usuario resolvidaPor; // Pode referenciar um Admin no futuro, usando a hierarquia Usuario

    @Column(name = "resolucao", columnDefinition = "TEXT")
    private String resolucao;

    @CreatedDate
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}