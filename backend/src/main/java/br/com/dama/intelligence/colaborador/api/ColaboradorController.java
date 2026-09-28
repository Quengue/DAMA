package br.com.dama.intelligence.colaborador.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ColaboradorController {

    private final ColaboradorFacade facade;

    public ColaboradorController(ColaboradorFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/empresas/{empresaId}/colaboradores")
    @PreAuthorize("hasAuthority('COLABORADOR_LER')")
    public List<ColaboradorView> listar(@PathVariable Long empresaId) {
        return facade.listarPorEmpresa(empresaId);
    }

    @PostMapping("/empresas/{empresaId}/colaboradores")
    @PreAuthorize("hasAuthority('COLABORADOR_ESCREVER')")
    public ResponseEntity<ColaboradorView> criar(@PathVariable Long empresaId,
                                                  @Valid @RequestBody CreateColaboradorCommand comando) {
        ColaboradorView criado = facade.criar(empresaId, comando);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @GetMapping("/colaboradores/{colaboradorId}")
    @PreAuthorize("hasAuthority('COLABORADOR_LER')")
    public ColaboradorView buscar(@PathVariable Long colaboradorId) {
        return facade.buscarPorId(colaboradorId);
    }

    @PutMapping("/colaboradores/{colaboradorId}")
    @PreAuthorize("hasAuthority('COLABORADOR_ESCREVER')")
    public ColaboradorView atualizar(@PathVariable Long colaboradorId,
                                      @Valid @RequestBody UpdateColaboradorCommand comando) {
        return facade.atualizar(colaboradorId, comando);
    }

    @PatchMapping("/colaboradores/{colaboradorId}/status")
    @PreAuthorize("hasAuthority('COLABORADOR_ESCREVER')")
    public ColaboradorView alterarStatus(@PathVariable Long colaboradorId,
                                          @Valid @RequestBody AlterarStatusCommand comando) {
        return facade.alterarStatus(colaboradorId, comando.ativo());
    }
}
