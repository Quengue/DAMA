package br.com.dama.intelligence.aicredit.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** RF25/RF26 — define um nível de autonomia de acesso à IA e o saldo mínimo de AI Credits que o desbloqueia. */
public record CreateNivelCommand(
        @NotBlank(message = "nome é obrigatório") String nome,
        @NotNull(message = "creditosMinimos é obrigatório")
        @PositiveOrZero(message = "creditosMinimos não pode ser negativo") BigDecimal creditosMinimos,
        @NotNull(message = "ordem é obrigatória") Integer ordem,
        String descricao
) {
}
