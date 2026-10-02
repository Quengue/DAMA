package br.com.dama.intelligence.analytics.internal;

import br.com.dama.intelligence.analytics.api.AlertaGestaoView;

import java.util.ArrayList;
import java.util.List;

/** Aplica as regras em Java ({@link AnaliseGestaoRegras}). É o motor padrão e o fallback quando o Node não responde. */
final class AlertaGestaoLocal implements AlertaGestaoClient {

    @Override
    public List<AlertaGestaoView> avaliar(EntradaAnalise entrada) {
        List<AlertaGestaoView> alertas = new ArrayList<>();
        entrada.metas().forEach(meta -> AnaliseGestaoRegras.avaliarMeta(meta, entrada.hoje()).ifPresent(alertas::add));
        entrada.tendencias().forEach(t ->
                AnaliseGestaoRegras.avaliarTendencia(t.anterior(), t.atual()).ifPresent(alertas::add));
        alertas.addAll(AnaliseGestaoRegras.avaliarEquipes(entrada.equipes()));
        entrada.semAtividade().forEach(c -> alertas.add(AnaliseGestaoRegras.semAtividade(c, entrada.hoje())));
        return alertas;
    }
}
