package br.com.dama.intelligence.analytics.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** RF15 / US24 — análises de desempenho para apoio à gestão, ordenadas da maior para a menor severidade. */
public record AnaliseGestaoView(
        Long empresaId,
        LocalDate dataReferencia,
        Map<String, Long> totalPorSeveridade,
        List<AlertaGestaoView> alertas
) {
}
