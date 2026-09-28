package br.com.dama.intelligence.conquista.api;

import java.time.LocalDateTime;

/** Conquista registrada no histórico do colaborador (RF31). concedidaPorId nulo = concessão automática. */
public record ConquistaObtidaView(
        Long colaboradorId,
        Long conquistaId,
        String nome,
        String descricao,
        String criterio,
        LocalDateTime obtidaEm,
        Long concedidaPorId
) {
}
