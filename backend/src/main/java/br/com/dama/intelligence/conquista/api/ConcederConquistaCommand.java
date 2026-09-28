package br.com.dama.intelligence.conquista.api;

import jakarta.validation.constraints.NotNull;

public record ConcederConquistaCommand(@NotNull(message = "conquistaId é obrigatório") Long conquistaId) {
}
