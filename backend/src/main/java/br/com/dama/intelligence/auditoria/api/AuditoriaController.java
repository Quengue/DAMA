package br.com.dama.intelligence.auditoria.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/empresas/{empresaId}/auditoria")
public class AuditoriaController {

    private final AuditoriaFacade facade;

    public AuditoriaController(AuditoriaFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERFIL_GERENCIAR')")
    public List<AuditoriaEventoView> listar(@PathVariable Long empresaId,
                                             @RequestParam(required = false) String entidade,
                                             @RequestParam(required = false) String entidadeId) {
        return facade.listar(empresaId, entidade, entidadeId);
    }
}
