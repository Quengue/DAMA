package br.com.dama.intelligence.analytics.api;

/**
 * Um ponto de atenção para o gestor (RF15). tipo: META_VENCIDA, META_EM_RISCO, INDICADOR_EM_QUEDA,
 * EQUIPE_ABAIXO_DA_MEDIA ou SEM_ATIVIDADE_RECENTE. severidade: ALTA, MEDIA ou BAIXA.
 * referenciaId aponta para a meta ou o indicador de origem, quando houver.
 */
public record AlertaGestaoView(
        String tipo,
        String severidade,
        String titulo,
        String detalhe,
        String recomendacao,
        Long colaboradorId,
        Long departamentoId,
        Long referenciaId
) {
}
