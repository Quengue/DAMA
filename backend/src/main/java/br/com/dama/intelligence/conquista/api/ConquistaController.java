package br.com.dama.intelligence.conquista.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ConquistaController {

    private final ConquistaFacade facade;

    public ConquistaController(ConquistaFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/conquistas/criterios")
    @PreAuthorize("hasAuthority('GAMIFICACAO_LER')")
    public List<Map<String, Object>> criterios() {
        return Arrays.stream(CriterioConquista.values())
                .map(c -> Map.<String, Object>of("codigo", c.name(), "descricao", c.descricao(), "automatico", c.automatico()))
                .toList();
    }

    @GetMapping("/empresas/{empresaId}/conquistas")
    @PreAuthorize("hasAuthority('GAMIFICACAO_LER')")
    public List<ConquistaView> listar(@PathVariable Long empresaId) {
        return facade.listar(empresaId);
    }

    @PostMapping("/empresas/{empresaId}/conquistas")
    @PreAuthorize("hasAuthority('GAMIFICACAO_ESCREVER')")
    public ResponseEntity<ConquistaView> criar(@PathVariable Long empresaId, @Valid @RequestBody CreateConquistaCommand comando) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.criar(empresaId, comando));
    }

    @PostMapping("/empresas/{empresaId}/conquistas/reavaliar")
    @PreAuthorize("hasAuthority('GAMIFICACAO_ESCREVER')")
    public Map<String, Integer> reavaliar(@PathVariable Long empresaId) {
        return Map.of("concedidas", facade.reavaliarEmpresa(empresaId));
    }

    @PatchMapping("/conquistas/{conquistaId}/ativa")
    @PreAuthorize("hasAuthority('GAMIFICACAO_ESCREVER')")
    public ConquistaView alterarAtiva(@PathVariable Long conquistaId, @Valid @RequestBody AlterarConquistaAtivaCommand comando) {
        return facade.alterarAtiva(conquistaId, comando.ativa());
    }

    @GetMapping("/colaboradores/{colaboradorId}/conquistas")
    @PreAuthorize("hasAuthority('GAMIFICACAO_LER')")
    public List<ConquistaObtidaView> doColaborador(@PathVariable Long colaboradorId) {
        return facade.conquistasDoColaborador(colaboradorId);
    }

    @PostMapping("/colaboradores/{colaboradorId}/conquistas")
    @PreAuthorize("hasAuthority('GAMIFICACAO_ESCREVER')")
    public ResponseEntity<ConquistaObtidaView> conceder(@PathVariable Long colaboradorId,
                                                         @Valid @RequestBody ConcederConquistaCommand comando) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.conceder(colaboradorId, comando.conquistaId()));
    }

    @GetMapping("/colaboradores/{colaboradorId}/evolucao")
    @PreAuthorize("hasAuthority('GAMIFICACAO_LER')")
    public EvolucaoView evolucao(@PathVariable Long colaboradorId) {
        return facade.evolucao(colaboradorId);
    }

    @GetMapping("/niveis-evolucao")
    @PreAuthorize("hasAuthority('GAMIFICACAO_LER')")
    public List<NivelEvolucaoView> listarNiveis() {
        return facade.listarNiveis();
    }

    @PostMapping("/niveis-evolucao")
    @PreAuthorize("hasAuthority('GAMIFICACAO_ESCREVER')")
    public ResponseEntity<NivelEvolucaoView> criarNivel(@Valid @RequestBody CreateNivelEvolucaoCommand comando) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.criarNivel(comando));
    }
}
