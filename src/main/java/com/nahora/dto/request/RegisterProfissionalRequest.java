package com.nahora.dto.request;

import java.util.List;

import org.hibernate.validator.constraints.br.CPF;

import com.nahora.model.enums.CategoriaServico;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegisterProfissionalRequest(
        // padrão para todo usuário
        @NotBlank(message = "O nome completo é obrigatório")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @NotBlank(message = "O telefone é obrigatório")
        String telefone,

        @NotBlank(message = "A senha é obrigatória")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,}$",
                message = "A senha deve ter no mínimo 8 caracteres, contendo pelo menos uma letra e um número"
        )
        String senha,

        // especificos de profissional
        @NotNull(message = "A categoria de serviço é obrigatória")
        CategoriaServico categoriaServico,

        @NotBlank @CPF(message = "CPF inválido")
        String cpf,

        @NotEmpty(message = "As especialidades são obrigatórias")
        List<String> especialidades,

        @NotNull(message = "Anos de experiência são obrigatórios")
        Integer anosExperiencia,

        @NotEmpty(message = "A área de atuação é obrigatória")
        List<String> areaAtuacao,

        @NotBlank(message = "A URL do RG (frente) é obrigatória")
        String rgFrenteUrl,

        @NotBlank(message = "A URL do RG (verso) é obrigatória")
        String rgVersoUrl,

        @NotBlank(message = "A URL da selfie é obrigatória")
        String selfieUrl


) {}