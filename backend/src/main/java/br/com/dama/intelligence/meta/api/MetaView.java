package br.com.dama.intelligence.meta.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MetaView(
        Long metaId,
        Long empresaId,
        Long indicadorId,
        Long colaboradorId,
        Long departamentoId,
        String descricao,
        BigDecimal valorAlvo,
        LocalDate dataInicio,
        LocalDate dataFim,
        String status,
        BigDecimal progressoAtual
) {
}
