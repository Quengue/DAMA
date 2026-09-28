package br.com.dama.intelligence.analytics.internal;

import br.com.dama.intelligence.analytics.api.*;
import br.com.dama.intelligence.analytics.internal.AnaliseGestaoRegras.ValorIndicador;
import br.com.dama.intelligence.shared.error.NotFoundException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

@Service
class AnalyticsService implements AnalyticsFacade {

    private final AnalyticsRepository repo;
    private final Clock clock;

    AnalyticsService(AnalyticsRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Override
    public VisaoConsolidadaView visaoConsolidada(Long empresaId) {
        validarEmpresa(empresaId);
        return repo.visaoConsolidada(empresaId)
                // empresa existe mas ainda não tem nenhum dado: devolve zeros em vez de 404
                .orElse(new VisaoConsolidadaView(empresaId, 0, 0, 0, 0, java.math.BigDecimal.ZERO));
    }

    @Override
    public List<RankingDepartamentoView> rankingDepartamentos(Long empresaId) {
        validarEmpresa(empresaId);
        return repo.rankingDepartamentos(empresaId);
    }

    @Override
    public List<RankingColaboradorView> rankingColaboradores(Long empresaId) {
        validarEmpresa(empresaId);
        return repo.rankingColaboradores(empresaId);
    }

    @Override
    public List<IndicadorDepartamentoView> indicadoresPorDepartamento(Long empresaId, Long indicadorId) {
        validarEmpresa(empresaId);
        return repo.indicadoresPorDepartamento(empresaId, indicadorId);
    }

    @Override
    public AnaliseGestaoView analisarGestao(Long empresaId) {
        validarEmpresa(empresaId);
        LocalDate hoje = LocalDate.now(clock);
        List<AlertaGestaoView> alertas = new ArrayList<>();

        repo.metasAbertas(empresaId).forEach(meta -> AnaliseGestaoRegras.avaliarMeta(meta, hoje).ifPresent(alertas::add));

        // valores vêm agrupados por série (indicador + colaborador/departamento), do mais recente ao anterior
        List<ValorIndicador> ultimos = repo.ultimosDoisValoresPorSerie(empresaId);
        for (int i = 0; i + 1 < ultimos.size(); i++) {
            ValorIndicador atual = ultimos.get(i);
            ValorIndicador anterior = ultimos.get(i + 1);
            if (mesmaSerie(atual, anterior)) {
                AnaliseGestaoRegras.avaliarTendencia(anterior, atual).ifPresent(alertas::add);
                i++;
            }
        }

        alertas.addAll(AnaliseGestaoRegras.avaliarEquipes(repo.equipes(empresaId)));

        repo.colaboradoresSemAtividade(empresaId, hoje.minusDays(AnaliseGestaoRegras.DIAS_SEM_ATIVIDADE))
                .forEach(c -> alertas.add(AnaliseGestaoRegras.semAtividade(c, hoje)));

        alertas.sort(Comparator.comparingInt((AlertaGestaoView a) -> AnaliseGestaoRegras.peso(a.severidade())));
        Map<String, Long> porSeveridade = new LinkedHashMap<>();
        for (String severidade : List.of("ALTA", "MEDIA", "BAIXA")) {
            porSeveridade.put(severidade, alertas.stream().filter(a -> a.severidade().equals(severidade)).count());
        }
        return new AnaliseGestaoView(empresaId, hoje, porSeveridade, alertas);
    }

    private static boolean mesmaSerie(ValorIndicador a, ValorIndicador b) {
        return a.indicadorId().equals(b.indicadorId())
                && Objects.equals(a.colaboradorId(), b.colaboradorId())
                && Objects.equals(a.departamentoId(), b.departamentoId());
    }

    private void validarEmpresa(Long empresaId) {
        if (!repo.existeEmpresa(empresaId)) {
            throw new NotFoundException("Empresa inexistente: " + empresaId);
        }
    }
}
