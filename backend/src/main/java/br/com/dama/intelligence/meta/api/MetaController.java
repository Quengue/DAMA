package br.com.dama.intelligence.meta.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MetaController {

    private final MetaFacade facade;

    public MetaController(MetaFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/empresas/{empresaId}/metas")
    @PreAuthorize("hasAuthority('DESEMPENHO_LER')")
    public List<MetaView> listar(@PathVariable Long empresaId,
                                  @RequestParam(required = false) Long colaboradorId,
                                  @RequestParam(required = false) Long departamentoId,
                                  @RequestParam(required = false) String status) {
        return facade.listar(empresaId, colaboradorId, departamentoId, status);
    }

    @PostMapping("/empresas/{empresaId}/metas")
    @PreAuthorize("hasAuthority('DESEMPENHO_ESCREVER')")
    public ResponseEntity<MetaView> criar(@PathVariable Long empresaId, @Valid @RequestBody CreateMetaCommand comando) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.criar(empresaId, comando));
    }

    @GetMapping("/metas/{metaId}")
    @PreAuthorize("hasAuthority('DESEMPENHO_LER')")
    public MetaView buscar(@PathVariable Long metaId) {
        return facade.buscar(metaId);
    }

    @PatchMapping("/metas/{metaId}/status")
    @PreAuthorize("hasAuthority('DESEMPENHO_ESCREVER')")
    public MetaView alterarStatus(@PathVariable Long metaId, @Valid @RequestBody AlterarStatusMetaCommand comando) {
        return facade.alterarStatus(metaId, comando.status());
    }

    @PostMapping("/metas/{metaId}/progresso")
    @PreAuthorize("hasAuthority('DESEMPENHO_ESCREVER')")
    public ResponseEntity<MetaProgressoView> registrarProgresso(@PathVariable Long metaId,
                                                                  @Valid @RequestBody RegistrarProgressoCommand comando) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.registrarProgresso(metaId, comando));
    }

    @GetMapping("/metas/{metaId}/progresso")
    @PreAuthorize("hasAuthority('DESEMPENHO_LER')")
    public List<MetaProgressoView> historicoProgresso(@PathVariable Long metaId) {
        return facade.historicoProgresso(metaId);
    }
}
