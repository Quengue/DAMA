package br.com.dama.intelligence.conquista.api;

import java.util.List;

/** RF31 — conquistas e níveis de evolução do colaborador. */
public interface ConquistaFacade {
    List<ConquistaView> listar(Long empresaId);

    /** Cria a conquista e, se for automática, já concede a quem cumpre o critério. */
    ConquistaView criar(Long empresaId, CreateConquistaCommand comando);

    ConquistaView alterarAtiva(Long conquistaId, boolean ativa);

    /** Concessão manual; só vale para conquistas com critério MANUAL. */
    ConquistaObtidaView conceder(Long colaboradorId, Long conquistaId);

    List<ConquistaObtidaView> conquistasDoColaborador(Long colaboradorId);

    /** Reavalia todas as conquistas automáticas da empresa; retorna quantas foram concedidas agora. */
    int reavaliarEmpresa(Long empresaId);

    EvolucaoView evolucao(Long colaboradorId);

    List<NivelEvolucaoView> listarNiveis();

    NivelEvolucaoView criarNivel(CreateNivelEvolucaoCommand comando);
}
