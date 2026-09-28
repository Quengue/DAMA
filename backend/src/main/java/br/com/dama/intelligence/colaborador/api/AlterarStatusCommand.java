package br.com.dama.intelligence.colaborador.api;

import jakarta.validation.constraints.NotNull;

public record AlterarStatusCommand(@NotNull(message = "ativo é obrigatório") Boolean ativo) {
}
