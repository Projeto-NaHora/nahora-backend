package com.nahora.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnderecoResponse {

    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String estado;
    private String cep;
    private Double latitude;
    private Double longitude;
}
