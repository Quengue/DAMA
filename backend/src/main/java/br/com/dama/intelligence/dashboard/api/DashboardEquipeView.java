package br.com.dama.intelligence.dashboard.api;

import java.math.BigDecimal;
import java.util.List;

/**
 * RF32/RF33 no nível de equipe (US15): indicadores do departamento, metas da equipe e de seus membros,
 * pontos, AI Credits e conquistas consolidados, e a posição no ranking de departamentos da empresa.
 */
public record DashboardEquipeView(
        Long departamentoId,
        String nome,
        Long empresaId,
        long colaboradoresAtivos,
        List<IndicadorResumoView> indicadores,
        long metasEmAndamento,
        long metasConcluidas,
        long metasVencidas,
        BigDecimal pontosTotais,
        BigDecimal aiCreditsSaldo,
        long conquistasObtidas,
        Long posicaoNoRanking,
        List<MembroEquipeView> membros
) {
}
