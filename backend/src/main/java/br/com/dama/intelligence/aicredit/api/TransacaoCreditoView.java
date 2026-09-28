package br.com.dama.intelligence.aicredit.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransacaoCreditoView(Long transacaoId, Long colaboradorId, Long regraId, Long registroId,
                                   String tipo, BigDecimal quantidade, String motivo, LocalDateTime registradoEm) {
}
