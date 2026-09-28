package br.com.dama.intelligence.desafio.api;

import jakarta.validation.constraints.NotBlank;

/** Valores aceitos: Planejado, Em andamento, Encerrado, Cancelado. */
public record AlterarStatusDesafioCommand(@NotBlank(message = "status é obrigatório") String status) {
}
