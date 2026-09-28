package br.com.dama.intelligence.dashboard.internal;

import br.com.dama.intelligence.colaborador.api.ColaboradorFacade;
import br.com.dama.intelligence.colaborador.api.ColaboradorView;
import br.com.dama.intelligence.dashboard.api.DashboardColaboradorView;
import br.com.dama.intelligence.dashboard.api.DashboardFacade;
import org.springframework.stereotype.Service;

@Service
class DashboardService implements DashboardFacade {

    private final ColaboradorFacade colaboradores;
    private final DashboardRepository repo;

    DashboardService(ColaboradorFacade colaboradores, DashboardRepository repo) {
        this.colaboradores = colaboradores;
        this.repo = repo;
    }

    @Override
    public DashboardColaboradorView doColaborador(Long colaboradorId) {
        ColaboradorView colaborador = colaboradores.buscarPorId(colaboradorId); // 404 se não existir

        return new DashboardColaboradorView(
                colaboradorId,
                colaborador.nome(),
                repo.ultimosIndicadores(colaboradorId),
                repo.contarMetas(colaboradorId, "Em andamento"),
                repo.contarMetas(colaboradorId, "Concluida"),
                repo.pontosTotais(colaboradorId),
                repo.aiCreditsSaldo(colaboradorId)
        );
    }
}
