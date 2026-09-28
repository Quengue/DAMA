package br.com.dama.intelligence.dashboard.api;

public interface DashboardFacade {
    DashboardColaboradorView doColaborador(Long colaboradorId);

    DashboardEquipeView daEquipe(Long departamentoId);
}
