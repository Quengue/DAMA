package br.com.dama.intelligence.analytics.api;

import java.math.BigDecimal;

/** RF08/RF28 — ranking de colaboradores com base na pontuação, dentro da empresa. */
public record RankingColaboradorView(
        Long colaboradorId,
        String nome,
        Long departamentoId,
        BigDecimal pontosTotais,
        long posicao
) {
}
