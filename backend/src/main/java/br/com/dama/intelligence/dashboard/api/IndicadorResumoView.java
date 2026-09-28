package br.com.dama.intelligence.dashboard.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IndicadorResumoView(String indicadorNome, String unidade, BigDecimal ultimoValor, LocalDate periodoReferencia) {
}
