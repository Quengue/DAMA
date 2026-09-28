package br.com.dama.intelligence.desafio.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/** RF09 — criar desafio. */
public record CreateDesafioCommand(
        @NotBlank(message = "nome é obrigatório") String nome,
        String descricao,
        @NotNull(message = "dataInicio é obrigatória") LocalDate dataInicio,
        @NotNull(message = "dataFim é obrigatória") LocalDate dataFim,
        @NotNull(message = "pontosRecompensa é obrigatório")
        @PositiveOrZero(message = "pontosRecompensa não pode ser negativo") BigDecimal pontosRecompensa
) {
}
