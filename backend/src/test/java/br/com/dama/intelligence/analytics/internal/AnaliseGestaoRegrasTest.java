package br.com.dama.intelligence.analytics.internal;

import br.com.dama.intelligence.analytics.api.AlertaGestaoView;
import br.com.dama.intelligence.analytics.internal.AnaliseGestaoRegras.Equipe;
import br.com.dama.intelligence.analytics.internal.AnaliseGestaoRegras.MetaAberta;
import br.com.dama.intelligence.analytics.internal.AnaliseGestaoRegras.ValorIndicador;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AnaliseGestaoRegrasTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 9, 28);

    private static MetaAberta meta(String alvo, String progresso, LocalDate inicio, LocalDate fim, boolean maiorMelhor) {
        return new MetaAberta(1L, "Entregas no prazo", 10L, "Lucas", null, null, new BigDecimal(alvo),
                progresso == null ? null : new BigDecimal(progresso), inicio, fim, maiorMelhor);
    }

    private static ValorIndicador valor(String v, LocalDate periodo, boolean maiorMelhor) {
        return new ValorIndicador(5L, "Produtividade", "%", maiorMelhor, 10L, "Lucas", null, null, periodo, new BigDecimal(v));
    }

    @Test
    void metaComPrazoEncerradoEhVencidaComSeveridadeAlta() {
        Optional<AlertaGestaoView> alerta = AnaliseGestaoRegras.avaliarMeta(
                meta("100", "40", HOJE.minusDays(60), HOJE.minusDays(1), true), HOJE);

        assertThat(alerta).get().extracting(AlertaGestaoView::tipo, AlertaGestaoView::severidade)
                .containsExactly("META_VENCIDA", "ALTA");
    }

    @Test
    void metaAtrasadaEmRelacaoAoTempoDecorridoFicaEmRisco() {
        // 80% do prazo decorrido e só 30% do alvo atingido
        Optional<AlertaGestaoView> alerta = AnaliseGestaoRegras.avaliarMeta(
                meta("100", "30", HOJE.minusDays(79), HOJE.plusDays(20), true), HOJE);

        assertThat(alerta).get().extracting(AlertaGestaoView::tipo).isEqualTo("META_EM_RISCO");
    }

    @Test
    void metaNoRitmoEsperadoNaoGeraAlerta() {
        assertThat(AnaliseGestaoRegras.avaliarMeta(meta("100", "70", HOJE.minusDays(79), HOJE.plusDays(20), true), HOJE))
                .isEmpty();
        // antes da metade do prazo nunca é considerada em risco
        assertThat(AnaliseGestaoRegras.avaliarMeta(meta("100", null, HOJE.minusDays(10), HOJE.plusDays(60), true), HOJE))
                .isEmpty();
    }

    @Test
    void metaDeIndicadorMenorEhMelhorNaoEntraNaRegraDeRisco() {
        assertThat(AnaliseGestaoRegras.avaliarMeta(meta("5", "30", HOJE.minusDays(79), HOJE.plusDays(20), false), HOJE))
                .isEmpty();
    }

    @Test
    void quedaDeVintePorCentoEhMediaEDeTrintaEhAlta() {
        Optional<AlertaGestaoView> media = AnaliseGestaoRegras.avaliarTendencia(
                valor("80", HOJE.minusMonths(1), true), valor("64", HOJE, true));
        Optional<AlertaGestaoView> alta = AnaliseGestaoRegras.avaliarTendencia(
                valor("80", HOJE.minusMonths(1), true), valor("56", HOJE, true));

        assertThat(media).get().extracting(AlertaGestaoView::severidade).isEqualTo("MEDIA");
        assertThat(alta).get().extracting(AlertaGestaoView::severidade).isEqualTo("ALTA");
        assertThat(alta.get().titulo()).contains("30%");
    }

    @Test
    void variacaoPequenaOuMelhoraNaoGeraAlerta() {
        assertThat(AnaliseGestaoRegras.avaliarTendencia(valor("80", HOJE.minusMonths(1), true), valor("75", HOJE, true)))
                .isEmpty();
        assertThat(AnaliseGestaoRegras.avaliarTendencia(valor("80", HOJE.minusMonths(1), true), valor("95", HOJE, true)))
                .isEmpty();
    }

    @Test
    void indicadorMenorEhMelhorPioraQuandoSobe() {
        assertThat(AnaliseGestaoRegras.avaliarTendencia(valor("10", HOJE.minusMonths(1), false), valor("13", HOJE, false)))
                .get().extracting(AlertaGestaoView::tipo, AlertaGestaoView::severidade)
                .containsExactly("INDICADOR_EM_QUEDA", "ALTA");
        assertThat(AnaliseGestaoRegras.avaliarTendencia(valor("10", HOJE.minusMonths(1), false), valor("7", HOJE, false)))
                .isEmpty();
    }

    @Test
    void equipeComMenosDaMetadeDaMediaDaEmpresaGeraAlerta() {
        List<AlertaGestaoView> alertas = AnaliseGestaoRegras.avaliarEquipes(List.of(
                new Equipe(1L, "Tecnologia", 4, new BigDecimal("800")),
                new Equipe(2L, "Comercial", 4, new BigDecimal("100")),
                new Equipe(3L, "Vazia", 0, BigDecimal.ZERO)));

        // média da empresa = 900 / 8 = 112,5; Comercial = 25 < 56,25
        assertThat(alertas).extracting(AlertaGestaoView::departamentoId).containsExactly(2L);
    }

    @Test
    void semPontosNaEmpresaNaoHaComparacaoEntreEquipes() {
        assertThat(AnaliseGestaoRegras.avaliarEquipes(List.of(
                new Equipe(1L, "A", 3, BigDecimal.ZERO), new Equipe(2L, "B", 2, BigDecimal.ZERO)))).isEmpty();
    }
}
