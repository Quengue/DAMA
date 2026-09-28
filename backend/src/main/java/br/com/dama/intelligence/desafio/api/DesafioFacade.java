package br.com.dama.intelligence.desafio.api;

import java.util.List;

/** Contrato público do módulo desafio (RF09). */
public interface DesafioFacade {
    List<DesafioView> listar(Long empresaId, String status);

    DesafioView buscar(Long desafioId);

    DesafioView criar(Long empresaId, CreateDesafioCommand comando);

    DesafioView alterarStatus(Long desafioId, String status);

    ParticipanteView inscrever(Long desafioId, Long colaboradorId);

    List<ParticipanteView> listarParticipantes(Long desafioId);

    /** Marca o participante como Concluido e credita os pontos de recompensa do desafio. */
    ParticipanteView concluir(Long desafioId, Long colaboradorId);

    ParticipanteView desistir(Long desafioId, Long colaboradorId);
}
