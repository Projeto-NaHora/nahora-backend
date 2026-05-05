package com.nahora.model;

import com.nahora.model.enums.TipoNotificacao;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacao")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "destinatario_id")
    private Usuario destinatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNotificacao tipo;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String mensagem;

    // Campos para navegação dinâmica no app mobile
    @Column(name = "entidade_relacionada_id")
    private Long entidadeRelacionadaId;

    @Column(name = "entidade_relacionada_tipo")
    private String entidadeRelacionadaTipo;

    private Boolean lida = false;

    @CreatedDate
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}