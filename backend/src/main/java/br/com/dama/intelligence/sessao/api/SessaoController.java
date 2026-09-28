package br.com.dama.intelligence.sessao.api;

import br.com.dama.intelligence.colaborador.api.ColaboradorFacade;
import br.com.dama.intelligence.colaborador.api.ColaboradorView;
import br.com.dama.intelligence.perfil.api.PerfilFacade;
import br.com.dama.intelligence.perfil.api.PerfilView;
import br.com.dama.intelligence.shared.security.UsuarioAtual;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** US01 (login): valida o colaborador do cabeçalho de autenticação e devolve perfis e permissões. */
@RestController
@RequestMapping("/api/me")
public class SessaoController {

    private final ColaboradorFacade colaboradores;
    private final PerfilFacade perfis;

    public SessaoController(ColaboradorFacade colaboradores, PerfilFacade perfis) {
        this.colaboradores = colaboradores;
        this.perfis = perfis;
    }

    @GetMapping
    public SessaoView me() {
        Long id = UsuarioAtual.id();
        ColaboradorView colaborador = colaboradores.buscarPorId(id); // 404 se o id não existir
        if (!colaborador.ativo()) {
            throw new AccessDeniedException("Colaborador desligado.");
        }
        List<String> nomesPerfis = perfis.perfisDoColaborador(id).stream().map(PerfilView::nome).toList();
        List<String> permissoes = perfis.permissoesDoColaborador(id).stream().sorted().toList();
        return new SessaoView(colaborador, nomesPerfis, permissoes);
    }
}
