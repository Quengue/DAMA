package br.com.dama.intelligence.colaborador.api;

import java.util.List;

/** Contrato público do módulo colaborador (RF01, RF02, RF16, RF17). */
public interface ColaboradorFacade {
    List<ColaboradorView> listarPorEmpresa(Long empresaId);

    ColaboradorView buscarPorId(Long colaboradorId);

    ColaboradorView criar(Long empresaId, CreateColaboradorCommand comando);

    ColaboradorView atualizar(Long colaboradorId, UpdateColaboradorCommand comando);

    ColaboradorView alterarStatus(Long colaboradorId, boolean ativo);

    /** Usado por outros módulos (perfil, pontuacao, meta...) para validar existência sem depender de detalhes internos. */
    boolean existe(Long colaboradorId);
}
