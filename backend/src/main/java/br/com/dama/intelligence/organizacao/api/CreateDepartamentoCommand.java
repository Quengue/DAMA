package br.com.dama.intelligence.organizacao.api;

import jakarta.validation.constraints.NotBlank;

public record CreateDepartamentoCommand(@NotBlank(message = "nome é obrigatório") String nome) {
}
