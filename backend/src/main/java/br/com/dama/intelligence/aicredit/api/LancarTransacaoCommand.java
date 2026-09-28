package br.com.dama.intelligence.aicredit.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** RF22 — atribuição manual (CREDITO) ou consumo (DEBITO) de AI Credits. */
public record LancarTransacaoCommand(
        @NotBlank(message = "tipo é obrigatório")
        @Pattern(regexp = "CREDITO|DEBITO", message = "tipo deve ser CREDITO ou DEBITO") String tipo,
        @NotNull(message = "quantidade é obrigatória")
        @Positive(message = "quantidade deve ser maior que zero") BigDecimal quantidade,
        @NotBlank(message = "motivo é obrigatório") String motivo
) {
}
