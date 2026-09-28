package br.com.dama.intelligence.aicredit.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AtividadeRegistroView(Long registroId, Long atividadeId, Long colaboradorId, BigDecimal quantidade,
                                    LocalDateTime registradoEm) {
}
