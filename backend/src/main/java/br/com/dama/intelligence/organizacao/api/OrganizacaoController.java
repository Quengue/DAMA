package br.com.dama.intelligence.organizacao.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OrganizacaoController {

    private final OrganizacaoFacade facade;

    public OrganizacaoController(OrganizacaoFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/empresas")
    @PreAuthorize("hasAuthority('COLABORADOR_LER')")
    public List<EmpresaView> listarEmpresas() {
        return facade.listarEmpresas();
    }

    @PostMapping("/empresas")
    @PreAuthorize("hasAuthority('PERFIL_GERENCIAR')")
    public ResponseEntity<EmpresaView> criarEmpresa(@Valid @RequestBody CreateEmpresaCommand comando) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.criarEmpresa(comando));
    }

    @GetMapping("/empresas/{empresaId}/departamentos")
    @PreAuthorize("hasAuthority('COLABORADOR_LER')")
    public List<DepartamentoView> listarDepartamentos(@PathVariable Long empresaId) {
        return facade.listarDepartamentos(empresaId);
    }

    @PostMapping("/empresas/{empresaId}/departamentos")
    @PreAuthorize("hasAuthority('PERFIL_GERENCIAR')")
    public ResponseEntity<DepartamentoView> criarDepartamento(@PathVariable Long empresaId,
                                                                @Valid @RequestBody CreateDepartamentoCommand comando) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.criarDepartamento(empresaId, comando));
    }
}
