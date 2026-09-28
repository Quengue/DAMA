package br.com.dama.intelligence.meta.api;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** RF30 — registrar o progresso atual de uma meta. */
public record RegistrarProgressoCommand(@NotNull(message = "valorAtual é obrigatório") BigDecimal valorAtual) {
}
