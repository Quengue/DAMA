package br.com.dama.intelligence.reconhecimento.api;

import java.util.List;

/** Contrato público do módulo reconhecimento (RF10). */
public interface ReconhecimentoFacade {
    List<ReconhecimentoView> listarPorEmpresa(Long empresaId);

    List<ReconhecimentoView> listarDoColaborador(Long colaboradorId);

    ReconhecimentoView criar(Long empresaId, CreateReconhecimentoCommand comando);
}
