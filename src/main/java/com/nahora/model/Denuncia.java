package com.nahora.model;

import com.nahora.model.enums.MotivoDenuncia;
import com.nahora.model.enums.StatusDenuncia;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "denuncia")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public class Denuncia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "denunciante_id")
    private Usuario denunciante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "denunciado_id")
    private Usuario denunciado;

    // Relacionamento opcional: pode ser uma denúncia geral do perfil ou atrelada a um pedido
    @ManyToOne
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MotivoDenuncia motivo;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusDenuncia status;

    @CreatedDate
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}