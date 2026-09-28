package br.com.dama.intelligence.reconhecimento.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReconhecimentoController {

    private final ReconhecimentoFacade facade;

    public ReconhecimentoController(ReconhecimentoFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/empresas/{empresaId}/reconhecimentos")
    @PreAuthorize("hasAuthority('GAMIFICACAO_LER')")
    public List<ReconhecimentoView> listarPorEmpresa(@PathVariable Long empresaId) {
        return facade.listarPorEmpresa(empresaId);
    }

    @PostMapping("/empresas/{empresaId}/reconhecimentos")
    @PreAuthorize("hasAuthority('GAMIFICACAO_ESCREVER')")
    public ResponseEntity<ReconhecimentoView> criar(@PathVariable Long empresaId,
                                                      @Valid @RequestBody CreateReconhecimentoCommand comando) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.criar(empresaId, comando));
    }

    @GetMapping("/colaboradores/{colaboradorId}/reconhecimentos")
    @PreAuthorize("hasAuthority('GAMIFICACAO_LER')")
    public List<ReconhecimentoView> listarDoColaborador(@PathVariable Long colaboradorId) {
        return facade.listarDoColaborador(colaboradorId);
    }
}
