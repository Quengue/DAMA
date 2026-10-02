package br.com.dama.intelligence.analytics.internal;

import br.com.dama.intelligence.analytics.api.AlertaGestaoView;
import br.com.dama.intelligence.analytics.internal.AnaliseGestaoRegras.ColaboradorSemAtividade;
import br.com.dama.intelligence.analytics.internal.AnaliseGestaoRegras.Equipe;
import br.com.dama.intelligence.analytics.internal.AnaliseGestaoRegras.MetaAberta;
import br.com.dama.intelligence.analytics.internal.AnaliseGestaoRegras.ValorIndicador;

import java.time.LocalDate;
import java.util.List;

/**
 * Porta para quem aplica as regras de alerta de gestão (RF15) sobre os dados lidos do banco.
 * Implementações: {@link AlertaGestaoNode} (serviço Node.js, ADR-0009) e {@link AlertaGestaoLocal} (regras em Java).
 * O {@code AnalyticsService} depende só desta interface, então trocar o motor não altera as regras de negócio expostas.
 */
interface AlertaGestaoClient {

    /** Devolve os alertas na ordem metas, tendências, equipes e sem atividade; a ordenação por severidade é do chamador. */
    List<AlertaGestaoView> avaliar(EntradaAnalise entrada);

    /** Dados brutos que alimentam as regras. */
    record EntradaAnalise(LocalDate hoje, List<MetaAberta> metas, List<Tendencia> tendencias, List<Equipe> equipes,
                          List<ColaboradorSemAtividade> semAtividade) {
    }

    /** Os dois últimos valores de uma mesma série de indicador. */
    record Tendencia(ValorIndicador anterior, ValorIndicador atual) {
    }
}
