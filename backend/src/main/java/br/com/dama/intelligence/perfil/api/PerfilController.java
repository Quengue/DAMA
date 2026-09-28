package br.com.dama.intelligence.perfil.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** RF18 — gerenciar perfis e permissões de acesso. */
@RestController
@RequestMapping("/api")
public class PerfilController {

    private final PerfilFacade facade;

    public PerfilController(PerfilFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/perfis")
    @PreAuthorize("hasAuthority('PERFIL_GERENCIAR')")
    public List<PerfilView> listarPerfis() {
        return facade.listarPerfis();
    }

    @PostMapping("/perfis")
    @PreAuthorize("hasAuthority('PERFIL_GERENCIAR')")
    public ResponseEntity<PerfilView> criarPerfil(@Valid @RequestBody CreatePerfilCommand comando) {
        PerfilView criado = facade.criarPerfil(comando);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @GetMapping("/permissoes")
    @PreAuthorize("hasAuthority('PERFIL_GERENCIAR')")
    public List<PermissaoView> listarPermissoes() {
        return facade.listarPermissoes();
    }

    @GetMapping("/colaboradores/{colaboradorId}/perfis")
    @PreAuthorize("hasAuthority('PERFIL_GERENCIAR')")
    public List<PerfilView> perfisDoColaborador(@PathVariable Long colaboradorId) {
        return facade.perfisDoColaborador(colaboradorId);
    }

    @PostMapping("/colaboradores/{colaboradorId}/perfis")
    @PreAuthorize("hasAuthority('PERFIL_GERENCIAR')")
    public ResponseEntity<Void> atribuirPerfil(@PathVariable Long colaboradorId,
                                                @Valid @RequestBody AtribuirPerfilCommand comando) {
        facade.atribuirPerfil(colaboradorId, comando.perfilId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/colaboradores/{colaboradorId}/perfis/{perfilId}")
    @PreAuthorize("hasAuthority('PERFIL_GERENCIAR')")
    public ResponseEntity<Void> removerPerfil(@PathVariable Long colaboradorId, @PathVariable Long perfilId) {
        facade.removerPerfil(colaboradorId, perfilId);
        return ResponseEntity.noContent().build();
    }
}
