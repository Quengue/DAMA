package br.com.dama.intelligence.desafio.api;

import jakarta.validation.constraints.NotNull;

public record InscreverParticipanteCommand(@NotNull(message = "colaboradorId é obrigatório") Long colaboradorId) {
}
