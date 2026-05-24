package com.nahora.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.nahora.model.enums.StatusMensagem;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensagem")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public class Mensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "conversa_id")
    private Conversa conversa;

    // Referência polimórfica: pode ser tanto Cliente quanto Profissional
    @ManyToOne(optional = false)
    @JoinColumn(name = "remetente_id")
    private Usuario remetente;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String conteudo;

    @Column(name = "anexo_url")
    private String anexoUrl;

    // NOVO STATUS
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusMensagem status = StatusMensagem.ENVIADA;

    // Flags para o filtro de Inteligência Artificial
    @Column(name = "bloqueada_ia")
    private Boolean bloqueadaIa = false;

    @Column(name = "motivo_bloqueio")
    private String motivoBloqueio;

    @CreatedDate
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}