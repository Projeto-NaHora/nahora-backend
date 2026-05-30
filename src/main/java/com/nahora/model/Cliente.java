package com.nahora.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cliente")
@Getter @Setter
public class Cliente extends Usuario {

    @ElementCollection
    @CollectionTable(name = "cliente_enderecos", joinColumns = @JoinColumn(name = "cliente_id"))
    private List<Endereco> enderecosSalvos = new ArrayList<>();

}