package br.com.dama.intelligence.conquista.api;

import java.math.BigDecimal;
import java.util.List;

/**
 * RF31 — perfil de evolução: nível atual (derivado dos pontos), quanto falta para o próximo e as conquistas obtidas.
 * proximoNivel é nulo quando o colaborador já está no nível mais alto.
 */
public record EvolucaoView(
        Long colaboradorId,
        String nome,
        BigDecimal pontosTotais,
        NivelEvolucaoView nivelAtual,
        NivelEvolucaoView proximoNivel,
        BigDecimal pontosParaProximoNivel,
        int percentualAteProximoNivel,
        List<ConquistaObtidaView> conquistas
) {
}
