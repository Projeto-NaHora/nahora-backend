package com.nahora.model.enums;

import lombok.Getter;

@Getter
public enum CategoriaServico {

    ELETRICA("Elétrica", "⚡"),
    PEDREIRO("Pedreiro", "🧱"),
    ENCANAMENTO("Encanamento", "🪠"),
    PINTURA("Pintura", "🎨"),
    MARCENARIA("Marcenaria", "🪵");

    private final String nome;
    private final String icone;

    CategoriaServico(String nome, String icone) {
        this.nome = nome;
        this.icone = icone;
    }
}
