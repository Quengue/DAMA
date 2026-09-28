package br.com.dama.intelligence.conquista.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateNivelEvolucaoCommand(
        @NotBlank(message = "nome é obrigatório") String nome,
        @NotNull(message = "pontosMinimos é obrigatório")
        @PositiveOrZero(message = "pontosMinimos não pode ser negativo") BigDecimal pontosMinimos,
        @NotNull(message = "ordem é obrigatória") Integer ordem,
        String descricao
) {
}
