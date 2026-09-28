package br.com.dama.intelligence.conquista.api;

import jakarta.validation.constraints.NotNull;

public record AlterarConquistaAtivaCommand(@NotNull(message = "ativa é obrigatório") Boolean ativa) {
}
