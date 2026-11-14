package org.fiap.com.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados de entrada para registrar uma avaliação (corpo do POST /avaliacao).
 *
 * @param descricao texto livre do aluno (obrigatório, até 1000 caracteres)
 * @param nota      inteiro de 0 a 10 (obrigatório)
 */
public record NovaAvaliacao(
        @NotBlank(message = "descricao é obrigatória")
        @Size(max = 1000, message = "descricao deve ter no máximo 1000 caracteres")
        String descricao,

        @NotNull(message = "nota é obrigatória")
        @Min(value = 0, message = "nota deve ser no mínimo 0")
        @Max(value = 10, message = "nota deve ser no máximo 10")
        Integer nota) {
}
