package br.com.dama.intelligence.aicredit.api;

import jakarta.validation.constraints.NotBlank;

/** RF19 — cadastrar atividade produtiva. */
public record CreateAtividadeCommand(@NotBlank(message = "nome é obrigatório") String nome, String descricao) {
}
