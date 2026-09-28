package br.com.dama.intelligence.dashboard.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/colaboradores/{colaboradorId}/dashboard")
public class DashboardController {

    private final DashboardFacade facade;

    public DashboardController(DashboardFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DESEMPENHO_LER')")
    public DashboardColaboradorView dashboard(@PathVariable Long colaboradorId) {
        return facade.doColaborador(colaboradorId);
    }
}
