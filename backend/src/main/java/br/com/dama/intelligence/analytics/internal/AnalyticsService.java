package br.com.dama.intelligence.analytics.internal;

import br.com.dama.intelligence.analytics.api.*;
import br.com.dama.intelligence.shared.error.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class AnalyticsService implements AnalyticsFacade {

    private final AnalyticsRepository repo;

    AnalyticsService(AnalyticsRepository repo) {
        this.repo = repo;
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

    private void validarEmpresa(Long empresaId) {
        if (!repo.existeEmpresa(empresaId)) {
            throw new NotFoundException("Empresa inexistente: " + empresaId);
        }
    }
}
