package br.com.dama.intelligence.pontuacao.api;

import java.math.BigDecimal;

public record RegraPontuacaoView(Long regraId, Long empresaId, Long atividadeId, String nome, BigDecimal pontos, boolean ativa) {
}
