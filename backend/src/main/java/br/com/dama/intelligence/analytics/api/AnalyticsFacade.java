package br.com.dama.intelligence.analytics.api;

import java.util.List;

/**
 * Módulo analytics readaptado: substitui a versão anterior, que comparava
 * empresas-cliente entre si (sem correspondência em nenhum RF). Aqui toda
 * comparação acontece DENTRO de uma única empresa — colaboradores e
 * departamentos, nunca entre empresas (RF13, RF14, RF15, RF36; também usado
 * por RF08/RF28 de ranking de colaboradores).
 */
public interface AnalyticsFacade {
    VisaoConsolidadaView visaoConsolidada(Long empresaId);

    List<RankingDepartamentoView> rankingDepartamentos(Long empresaId);

    List<RankingColaboradorView> rankingColaboradores(Long empresaId);

    /** RF33/RF36: valor mais recente de cada indicador por equipe; indicadorId opcional para comparar um indicador só. */
    List<IndicadorDepartamentoView> indicadoresPorDepartamento(Long empresaId, Long indicadorId);

    /** RF15/US24: metas vencidas ou em risco, indicadores em queda, equipes abaixo da média e colaboradores sem atividade. */
    AnaliseGestaoView analisarGestao(Long empresaId);
}
