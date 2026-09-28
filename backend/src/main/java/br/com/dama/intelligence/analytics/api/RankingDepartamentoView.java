package br.com.dama.intelligence.analytics.api;

import java.math.BigDecimal;

/** RF14/RF36 — comparação de resultados entre equipes (departamentos) DENTRO de uma empresa. */
public record RankingDepartamentoView(
        Long departamentoId,
        String departamentoNome,
        long colaboradoresAtivos,
        BigDecimal pontosTotais,
        BigDecimal pontosMediaPorLancamento,
        long posicao
) {
}
