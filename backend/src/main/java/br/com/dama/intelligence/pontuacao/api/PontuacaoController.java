package br.com.dama.intelligence.pontuacao.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PontuacaoController {

    private final PontuacaoFacade facade;

    public PontuacaoController(PontuacaoFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/empresas/{empresaId}/regras-pontuacao")
    @PreAuthorize("hasAuthority('GAMIFICACAO_LER')")
    public List<RegraPontuacaoView> listarRegras(@PathVariable Long empresaId) {
        return facade.listarRegras(empresaId);
    }

    @PostMapping("/empresas/{empresaId}/regras-pontuacao")
    @PreAuthorize("hasAuthority('GAMIFICACAO_ESCREVER')")
    public ResponseEntity<RegraPontuacaoView> criarRegra(@PathVariable Long empresaId,
                                                           @Valid @RequestBody CreateRegraPontuacaoCommand comando) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.criarRegra(empresaId, comando));
    }

    @PatchMapping("/regras-pontuacao/{regraId}/ativa")
    @PreAuthorize("hasAuthority('GAMIFICACAO_ESCREVER')")
    public RegraPontuacaoView alterarRegraAtiva(@PathVariable Long regraId,
                                                  @Valid @RequestBody AlterarRegraAtivaCommand comando) {
        return facade.alterarRegraAtiva(regraId, comando.ativa());
    }

    @PostMapping("/colaboradores/{colaboradorId}/pontuacao")
    @PreAuthorize("hasAuthority('GAMIFICACAO_ESCREVER')")
    public ResponseEntity<PontuacaoView> lancar(@PathVariable Long colaboradorId,
                                                  @Valid @RequestBody LancarPontosCommand comando) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.lancar(colaboradorId, comando));
    }

    @GetMapping("/colaboradores/{colaboradorId}/pontuacao")
    @PreAuthorize("hasAuthority('GAMIFICACAO_LER')")
    public PontuacaoResumoView resumo(@PathVariable Long colaboradorId) {
        return facade.resumo(colaboradorId);
    }
}
