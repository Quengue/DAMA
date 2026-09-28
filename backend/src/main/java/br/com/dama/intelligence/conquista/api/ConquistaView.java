package br.com.dama.intelligence.conquista.api;

import java.math.BigDecimal;

public record ConquistaView(
        Long conquistaId,
        Long empresaId,
        String nome,
        String descricao,
        String criterio,
        BigDecimal valorMinimo,
        boolean ativa,
        long colaboradoresQueObtiveram
) {
}
