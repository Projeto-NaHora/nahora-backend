package com.nahora.dto.request;

import com.nahora.model.enums.CategoriaServico;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProfissionalPerfilRequest(
        String nome,
        String email,
        String cpf,
        String celular,
        String fotoPerfil,
        String profissao,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String estado,

        @Min(value = 0, message = "Os anos de experiência não podem ser negativos.")
        Integer anosExperiencia,

        String bio,

        List<String> especialidades,

        List<CategoriaServico> categorias,

        @Positive(message = "O raio de atuação deve ser maior que zero.")
        Double raioAtuacaoKm,

        Double latitude,

        Double longitude,

        @Size(max = 10, message = "Máximo de 10 fotos no portfólio.")
        List<String> urlsFotosPortfolio
) {}
