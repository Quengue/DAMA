package br.com.dama.intelligence.meta.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MetaProgressoView(Long metaProgressoId, Long metaId, BigDecimal valorAtual, LocalDateTime registradoEm) {
}
