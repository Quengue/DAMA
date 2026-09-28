package br.com.dama.intelligence.perfil.api;

import java.util.List;
import java.util.Set;

/**
 * Contrato público do módulo perfil (RF18, RNF02, RN01, RN07).
 * Consumido pelo mecanismo de autorização em shared.security e por
 * qualquer módulo que precise saber "o que este colaborador pode fazer".
 */
public interface PerfilFacade {
    List<PerfilView> listarPerfis();

    PerfilView criarPerfil(CreatePerfilCommand comando);

    List<PermissaoView> listarPermissoes();

    void atribuirPerfil(Long colaboradorId, Long perfilId);

    void removerPerfil(Long colaboradorId, Long perfilId);

    List<PerfilView> perfisDoColaborador(Long colaboradorId);

    /** Usado pelo filtro de autenticação para montar as authorities do colaborador autenticado. */
    Set<String> permissoesDoColaborador(Long colaboradorId);
}
