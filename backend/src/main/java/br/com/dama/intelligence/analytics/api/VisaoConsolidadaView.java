package br.com.dama.intelligence.analytics.api;

import java.math.BigDecimal;

/** RF13 — visão consolidada do desempenho de uma empresa. */
public record VisaoConsolidadaView(
        Long empresaId,
        long colaboradoresAtivos,
        long totalDepartamentos,
        long metasEmAndamento,
        long metasConcluidas,
        BigDecimal pontosTotaisPeriodo
) {
}
