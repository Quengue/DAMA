package br.com.dama.intelligence.pontuacao.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** RN02 — critério definido de pontos. atividadeId é opcional (regra pode não estar ligada a uma atividade produtiva). */
public record CreateRegraPontuacaoCommand(
        Long atividadeId,
        @NotBlank(message = "nome é obrigatório") String nome,
        @NotNull(message = "pontos é obrigatório") BigDecimal pontos
) {
}
