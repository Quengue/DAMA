package br.com.dama.intelligence.pontuacao.api;

import jakarta.validation.constraints.NotNull;

/** RF07 — lançar pontos a um colaborador. Os pontos vêm sempre de uma regra (RN02); a descrição é opcional. */
public record LancarPontosCommand(@NotNull(message = "regraId é obrigatório") Long regraId, String descricao) {
}
