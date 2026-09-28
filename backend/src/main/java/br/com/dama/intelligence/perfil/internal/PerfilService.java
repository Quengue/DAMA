package br.com.dama.intelligence.perfil.internal;

import br.com.dama.intelligence.perfil.api.*;
import br.com.dama.intelligence.shared.error.NotFoundException;
import br.com.dama.intelligence.shared.error.ValidationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
class PerfilService implements PerfilFacade {

    private final PerfilRepository repo;

    PerfilService(PerfilRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<PerfilView> listarPerfis() {
        return repo.listarPerfis();
    }

    @Override
    public PerfilView criarPerfil(CreatePerfilCommand comando) {
        List<Long> permissaoIds = repo.resolverPermissoes(comando.permissoes());
        if (permissaoIds.size() != comando.permissoes().size()) {
            throw new ValidationException("Um ou mais códigos de permissão informados não existem.");
        }
        Long perfilId = repo.inserirPerfil(comando.nome(), comando.descricao());
        repo.vincularPermissoes(perfilId, permissaoIds);
        return new PerfilView(perfilId, comando.nome(), comando.descricao(), repo.permissoesDoPerfil(perfilId));
    }

    @Override
    public List<PermissaoView> listarPermissoes() {
        return repo.listarPermissoes();
    }

    @Override
    public void atribuirPerfil(Long colaboradorId, Long perfilId) {
        if (!repo.existePerfil(perfilId)) {
            throw new NotFoundException("Perfil inexistente: " + perfilId);
        }
        repo.atribuirPerfil(colaboradorId, perfilId);
    }

    @Override
    public void removerPerfil(Long colaboradorId, Long perfilId) {
        repo.removerPerfil(colaboradorId, perfilId);
    }

    @Override
    public List<PerfilView> perfisDoColaborador(Long colaboradorId) {
        return repo.perfisDoColaborador(colaboradorId);
    }

    @Override
    public Set<String> permissoesDoColaborador(Long colaboradorId) {
        return repo.permissoesDoColaborador(colaboradorId);
    }
}
