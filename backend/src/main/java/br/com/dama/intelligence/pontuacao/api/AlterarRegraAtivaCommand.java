package br.com.dama.intelligence.pontuacao.api;

import jakarta.validation.constraints.NotNull;

public record AlterarRegraAtivaCommand(@NotNull(message = "ativa é obrigatório") Boolean ativa) {
}
