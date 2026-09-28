package br.com.dama.intelligence.indicador.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IndicadorValorView(
        Long indicadorValorId,
        Long indicadorId,
        Long colaboradorId,
        Long departamentoId,
        LocalDate periodoReferencia,
        BigDecimal valor
) {
}
