package br.com.dama.intelligence.aicredit.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** RF20 — registrar atividade realizada. quantidade é opcional (padrão 1). */
public record RegistrarAtividadeCommand(
        @NotNull(message = "atividadeId é obrigatório") Long atividadeId,
        @Positive(message = "quantidade deve ser maior que zero") BigDecimal quantidade
) {
}
