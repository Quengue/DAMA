package br.com.dama.intelligence.desafio.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DesafioController {

    private final DesafioFacade facade;

    public DesafioController(DesafioFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/empresas/{empresaId}/desafios")
    @PreAuthorize("hasAuthority('GAMIFICACAO_LER')")
    public List<DesafioView> listar(@PathVariable Long empresaId, @RequestParam(required = false) String status) {
        return facade.listar(empresaId, status);
    }

    @PostMapping("/empresas/{empresaId}/desafios")
    @PreAuthorize("hasAuthority('GAMIFICACAO_ESCREVER')")
    public ResponseEntity<DesafioView> criar(@PathVariable Long empresaId, @Valid @RequestBody CreateDesafioCommand comando) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.criar(empresaId, comando));
    }

    @GetMapping("/desafios/{desafioId}")
    @PreAuthorize("hasAuthority('GAMIFICACAO_LER')")
    public DesafioView buscar(@PathVariable Long desafioId) {
        return facade.buscar(desafioId);
    }

    @PatchMapping("/desafios/{desafioId}/status")
    @PreAuthorize("hasAuthority('GAMIFICACAO_ESCREVER')")
    public DesafioView alterarStatus(@PathVariable Long desafioId, @Valid @RequestBody AlterarStatusDesafioCommand comando) {
        return facade.alterarStatus(desafioId, comando.status());
    }

    @GetMapping("/desafios/{desafioId}/participantes")
    @PreAuthorize("hasAuthority('GAMIFICACAO_LER')")
    public List<ParticipanteView> listarParticipantes(@PathVariable Long desafioId) {
        return facade.listarParticipantes(desafioId);
    }

    @PostMapping("/desafios/{desafioId}/participantes")
    @PreAuthorize("hasAuthority('GAMIFICACAO_ESCREVER')")
    public ResponseEntity<ParticipanteView> inscrever(@PathVariable Long desafioId,
                                                        @Valid @RequestBody InscreverParticipanteCommand comando) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.inscrever(desafioId, comando.colaboradorId()));
    }

    @PostMapping("/desafios/{desafioId}/participantes/{colaboradorId}/concluir")
    @PreAuthorize("hasAuthority('GAMIFICACAO_ESCREVER')")
    public ParticipanteView concluir(@PathVariable Long desafioId, @PathVariable Long colaboradorId) {
        return facade.concluir(desafioId, colaboradorId);
    }

    @PostMapping("/desafios/{desafioId}/participantes/{colaboradorId}/desistir")
    @PreAuthorize("hasAuthority('GAMIFICACAO_ESCREVER')")
    public ParticipanteView desistir(@PathVariable Long desafioId, @PathVariable Long colaboradorId) {
        return facade.desistir(desafioId, colaboradorId);
    }
}
