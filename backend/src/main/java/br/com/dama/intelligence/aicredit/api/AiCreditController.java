package br.com.dama.intelligence.aicredit.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AiCreditController {

    private final AiCreditFacade facade;

    public AiCreditController(AiCreditFacade facade) {
        this.facade = facade;
    }

    // ---- RF19: atividades produtivas ----------------------------------------------------------

    @GetMapping("/empresas/{empresaId}/atividades-produtivas")
    @PreAuthorize("hasAuthority('AI_CREDITS_LER')")
    public List<AtividadeProdutivaView> listarAtividades(@PathVariable Long empresaId) {
        return facade.listarAtividades(empresaId);
    }

    @PostMapping("/empresas/{empresaId}/atividades-produtivas")
    @PreAuthorize("hasAuthority('AI_CREDITS_GERENCIAR')")
    public ResponseEntity<AtividadeProdutivaView> criarAtividade(@PathVariable Long empresaId,
                                                                   @Valid @RequestBody CreateAtividadeCommand comando) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.criarAtividade(empresaId, comando));
    }

    // ---- RF20: atividades realizadas ---------------------------------------------------------

    @PostMapping("/colaboradores/{colaboradorId}/atividades")
    @PreAuthorize("hasAuthority('AI_CREDITS_GERENCIAR')")
    public ResponseEntity<RegistroAtividadeResultadoView> registrarAtividade(@PathVariable Long colaboradorId,
                                                                               @Valid @RequestBody RegistrarAtividadeCommand comando) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.registrarAtividade(colaboradorId, comando));
    }

    @GetMapping("/colaboradores/{colaboradorId}/atividades")
    @PreAuthorize("hasAuthority('AI_CREDITS_LER')")
    public List<AtividadeRegistroView> listarRegistros(@PathVariable Long colaboradorId) {
        return facade.listarRegistros(colaboradorId);
    }

    // ---- RF21: regras e elegibilidade ---------------------------------------------------------

    @GetMapping("/empresas/{empresaId}/ai-credits/regras")
    @PreAuthorize("hasAuthority('AI_CREDITS_LER')")
    public List<RegraCreditoView> listarRegras(@PathVariable Long empresaId) {
        return facade.listarRegras(empresaId);
    }

    @PostMapping("/empresas/{empresaId}/ai-credits/regras")
    @PreAuthorize("hasAuthority('AI_CREDITS_GERENCIAR')")
    public ResponseEntity<RegraCreditoView> criarRegra(@PathVariable Long empresaId,
                                                         @Valid @RequestBody CreateRegraCreditoCommand comando) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.criarRegra(empresaId, comando));
    }

    @GetMapping("/atividades-produtivas/{atividadeId}/elegibilidade")
    @PreAuthorize("hasAuthority('AI_CREDITS_LER')")
    public ElegibilidadeView avaliarElegibilidade(@PathVariable Long atividadeId) {
        return facade.avaliarElegibilidade(atividadeId);
    }

    // ---- RF22/RF23/RF24: transações, saldo e histórico ---------------------------------------

    @PostMapping("/colaboradores/{colaboradorId}/ai-credits/transacoes")
    @PreAuthorize("hasAuthority('AI_CREDITS_GERENCIAR')")
    public ResponseEntity<TransacaoCreditoView> lancarTransacao(@PathVariable Long colaboradorId,
                                                                  @Valid @RequestBody LancarTransacaoCommand comando) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.lancarTransacao(colaboradorId, comando));
    }

    @GetMapping("/colaboradores/{colaboradorId}/ai-credits/historico")
    @PreAuthorize("hasAuthority('AI_CREDITS_LER')")
    public List<TransacaoCreditoView> historico(@PathVariable Long colaboradorId) {
        return facade.historico(colaboradorId);
    }

    @GetMapping("/colaboradores/{colaboradorId}/ai-credits/saldo")
    @PreAuthorize("hasAuthority('AI_CREDITS_LER')")
    public SaldoCreditosView saldo(@PathVariable Long colaboradorId) {
        return facade.saldo(colaboradorId);
    }

    // ---- RF25/RF26: níveis de autonomia ------------------------------------------------------

    @GetMapping("/ai-credits/niveis")
    @PreAuthorize("hasAuthority('AI_CREDITS_LER')")
    public List<NivelAutonomiaView> listarNiveis() {
        return facade.listarNiveis();
    }

    @PostMapping("/ai-credits/niveis")
    @PreAuthorize("hasAuthority('AI_CREDITS_GERENCIAR')")
    public ResponseEntity<NivelAutonomiaView> criarNivel(@Valid @RequestBody CreateNivelCommand comando) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.criarNivel(comando));
    }
}
