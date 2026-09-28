package br.com.dama.intelligence.aicredit.api;

import java.util.List;

/** RF21 — avaliação de elegibilidade de uma atividade para gerar AI Credits. */
public record ElegibilidadeView(Long atividadeId, boolean elegivel, List<RegraCreditoView> regrasAplicaveis) {
}
