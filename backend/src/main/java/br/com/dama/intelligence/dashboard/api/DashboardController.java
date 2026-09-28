package br.com.dama.intelligence.dashboard.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardFacade facade;

    public DashboardController(DashboardFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/colaboradores/{colaboradorId}/dashboard")
    @PreAuthorize("hasAuthority('DESEMPENHO_LER')")
    public DashboardColaboradorView dashboard(@PathVariable Long colaboradorId) {
        return facade.doColaborador(colaboradorId);
    }

    @GetMapping("/departamentos/{departamentoId}/dashboard")
    @PreAuthorize("hasAuthority('DESEMPENHO_LER')")
    public DashboardEquipeView dashboardEquipe(@PathVariable Long departamentoId) {
        return facade.daEquipe(departamentoId);
    }
}
