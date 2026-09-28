package br.com.dama.intelligence.aicredit.api;

import java.math.BigDecimal;

public record RegraCreditoView(Long regraId, Long empresaId, Long atividadeId, String nome,
                               String criterioElegibilidade, BigDecimal creditosConcedidos, boolean ativa) {
}
