package br.com.dama.intelligence.dashboard.api;

import java.math.BigDecimal;
import java.util.List;

/** RF05 — dashboard com indicadores de desempenho de um colaborador. RNF04: lê sempre dos dados de origem, nunca duplica. */
public record DashboardColaboradorView(
        Long colaboradorId,
        String nome,
        List<IndicadorResumoView> indicadores,
        long metasEmAndamento,
        long metasConcluidas,
        BigDecimal pontosTotais,
        BigDecimal aiCreditsSaldo
) {
}
