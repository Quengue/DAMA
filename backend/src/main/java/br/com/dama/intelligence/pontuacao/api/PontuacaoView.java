package br.com.dama.intelligence.pontuacao.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PontuacaoView(Long pontuacaoId, Long colaboradorId, Long regraId, BigDecimal pontos,
                            String descricao, LocalDateTime registradoEm) {
}
