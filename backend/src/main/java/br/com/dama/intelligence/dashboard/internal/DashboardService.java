package br.com.dama.intelligence.dashboard.internal;

import br.com.dama.intelligence.colaborador.api.ColaboradorFacade;
import br.com.dama.intelligence.colaborador.api.ColaboradorView;
import br.com.dama.intelligence.conquista.api.ConquistaFacade;
import br.com.dama.intelligence.conquista.api.EvolucaoView;
import br.com.dama.intelligence.dashboard.api.DashboardColaboradorView;
import br.com.dama.intelligence.dashboard.api.DashboardEquipeView;
import br.com.dama.intelligence.dashboard.api.DashboardFacade;
import br.com.dama.intelligence.organizacao.api.DepartamentoView;
import br.com.dama.intelligence.organizacao.api.OrganizacaoFacade;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

@Service
class DashboardService implements DashboardFacade {

    private final ColaboradorFacade colaboradores;
    private final OrganizacaoFacade organizacao;
    private final ConquistaFacade conquistas;
    private final DashboardRepository repo;
    private final Clock clock;

    DashboardService(ColaboradorFacade colaboradores, OrganizacaoFacade organizacao, ConquistaFacade conquistas,
                     DashboardRepository repo, Clock clock) {
        this.colaboradores = colaboradores;
        this.organizacao = organizacao;
        this.conquistas = conquistas;
        this.repo = repo;
        this.clock = clock;
    }

    @Override
    public DashboardColaboradorView doColaborador(Long colaboradorId) {
        ColaboradorView colaborador = colaboradores.buscarPorId(colaboradorId); // 404 se não existir
        EvolucaoView evolucao = conquistas.evolucao(colaboradorId);

        return new DashboardColaboradorView(
                colaboradorId,
                colaborador.nome(),
                repo.ultimosIndicadores(colaboradorId),
                repo.contarMetas(colaboradorId, "Em andamento"),
                repo.contarMetas(colaboradorId, "Concluida"),
                repo.pontosTotais(colaboradorId),
                repo.aiCreditsSaldo(colaboradorId),
                evolucao.nivelAtual() == null ? null : evolucao.nivelAtual().nome(),
                evolucao.conquistas().size()
        );
    }

    @Override
    public DashboardEquipeView daEquipe(Long departamentoId) {
        DepartamentoView departamento = organizacao.buscarDepartamento(departamentoId); // 404 se não existir
        LocalDate hoje = LocalDate.now(clock);

        return new DashboardEquipeView(
                departamentoId,
                departamento.nome(),
                departamento.empresaId(),
                repo.colaboradoresAtivosDaEquipe(departamentoId),
                repo.ultimosIndicadoresDaEquipe(departamentoId),
                repo.contarMetasDaEquipe(departamentoId, "Em andamento"),
                repo.contarMetasDaEquipe(departamentoId, "Concluida"),
                repo.contarMetasVencidasDaEquipe(departamentoId, hoje),
                repo.pontosDaEquipe(departamentoId),
                repo.aiCreditsSaldoDaEquipe(departamentoId),
                repo.conquistasDaEquipe(departamentoId),
                repo.posicaoNoRanking(departamentoId),
                repo.membros(departamentoId)
        );
    }
}
