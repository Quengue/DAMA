package br.com.dama.intelligence.aicredit.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** RF21/RN04 — regra que define quando uma atividade é elegível a gerar AI Credits e quantos. */
public record CreateRegraCreditoCommand(
        @NotNull(message = "atividadeId é obrigatório") Long atividadeId,
        @NotBlank(message = "nome é obrigatório") String nome,
        @NotBlank(message = "criterioElegibilidade é obrigatório") String criterioElegibilidade,
        @NotNull(message = "creditosConcedidos é obrigatório")
        @Positive(message = "creditosConcedidos deve ser maior que zero") BigDecimal creditosConcedidos
) {
}
