package br.com.dama.intelligence.pontuacao.api;

import java.math.BigDecimal;
import java.util.List;

/** RF27/RF11 — total e histórico de pontuação de um colaborador (mais recentes primeiro). */
public record PontuacaoResumoView(Long colaboradorId, BigDecimal pontosTotais, List<PontuacaoView> historico) {
}
