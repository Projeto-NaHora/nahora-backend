package com.nahora.dto.request;

import com.nahora.model.enums.CategoriaServico;
import jakarta.validation.constraints.*;
import java.util.List;

public record CompletarPerfilRequestDTO(
        @NotBlank(message = "A bio é obrigatória.")
        String bio,

        @NotEmpty(message = "A lista de categorias não pode estar vazia.")
        List<CategoriaServico> categorias,

        List<String> especialidades,

        @NotNull(message = "Os anos de experiência são obrigatórios.")
        @Min(value = 0, message = "Os anos de experiência não podem ser negativos.")
        Integer anosExperiencia,

        @NotNull(message = "O raio de atuação é obrigatório.")
        @Positive(message = "O raio de atuação deve ser maior que zero.")
        Double raioAtuacaoKm,

        @NotNull(message = "A latitude é obrigatória.")
        Double latitude,

        @NotNull(message = "A longitude é obrigatória.")
        Double longitude,

        @Size(max = 10, message = "Máximo de 10 fotos no portfólio.")
        List<String> urlsFotos
) {
}