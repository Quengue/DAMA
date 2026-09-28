package br.com.dama.intelligence.indicador.api;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class IndicadorController {

    private final IndicadorFacade facade;

    public IndicadorController(IndicadorFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/empresas/{empresaId}/indicadores")
    @PreAuthorize("hasAuthority('DESEMPENHO_LER')")
    public List<IndicadorView> listar(@PathVariable Long empresaId) {
        return facade.listarPorEmpresa(empresaId);
    }

    @PostMapping("/empresas/{empresaId}/indicadores")
    @PreAuthorize("hasAuthority('DESEMPENHO_ESCREVER')")
    public ResponseEntity<IndicadorView> criar(@PathVariable Long empresaId,
                                                @Valid @RequestBody CreateIndicadorCommand comando) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.criar(empresaId, comando));
    }

    @PostMapping("/indicadores/{indicadorId}/valores")
    @PreAuthorize("hasAuthority('DESEMPENHO_ESCREVER')")
    public ResponseEntity<IndicadorValorView> registrarValor(@PathVariable Long indicadorId,
                                                               @Valid @RequestBody RegistrarValorCommand comando) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.registrarValor(indicadorId, comando));
    }

    @GetMapping("/indicadores/{indicadorId}/valores")
    @PreAuthorize("hasAuthority('DESEMPENHO_LER')")
    public List<IndicadorValorView> historico(@PathVariable Long indicadorId,
                                               @RequestParam(required = false) Long colaboradorId,
                                               @RequestParam(required = false) Long departamentoId,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return facade.historico(indicadorId, colaboradorId, departamentoId, inicio, fim);
    }
}
