package com.nahora.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

@Embeddable
@Getter @Setter
public class Endereco {

    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String estado;
    private String cep;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point coordenadas;
}
