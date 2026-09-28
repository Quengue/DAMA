package br.com.dama.intelligence.meta.api;

import java.util.List;

/** Contrato público do módulo meta (RF04, RF29, RF30, RN05). */
public interface MetaFacade {
    List<MetaView> listar(Long empresaId, Long colaboradorId, Long departamentoId, String status);

    MetaView buscar(Long metaId);

    MetaView criar(Long empresaId, CreateMetaCommand comando);

    MetaView alterarStatus(Long metaId, String status);

    MetaProgressoView registrarProgresso(Long metaId, RegistrarProgressoCommand comando);

    List<MetaProgressoView> historicoProgresso(Long metaId);
}
