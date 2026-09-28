package br.com.dama.intelligence.indicador.api;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** RF03 — registrar um valor do indicador para um colaborador OU um departamento (nunca os dois nem nenhum). */
public record RegistrarValorCommand(
        Long colaboradorId,
        Long departamentoId,
        @NotNull(message = "periodoReferencia é obrigatório") LocalDate periodoReferencia,
        @NotNull(message = "valor é obrigatório") BigDecimal valor,
        Long registradoPor
) {
}
