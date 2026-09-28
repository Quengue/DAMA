package br.com.dama.intelligence.analytics.api;

import java.math.BigDecimal;
import java.time.LocalDate;

/** RF33/RF36 — último valor registrado de um indicador para cada equipe (departamento) da empresa. */
public record IndicadorDepartamentoView(
        Long departamentoId,
        String departamentoNome,
        Long indicadorId,
        String indicadorNome,
        String unidade,
        LocalDate periodoReferencia,
        BigDecimal valor
) {
}
