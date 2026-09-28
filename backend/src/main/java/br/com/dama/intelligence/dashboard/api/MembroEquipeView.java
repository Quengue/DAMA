package br.com.dama.intelligence.dashboard.api;

import java.math.BigDecimal;

public record MembroEquipeView(Long colaboradorId, String nome, String cargo, BigDecimal pontosTotais,
                               BigDecimal aiCreditsSaldo, long conquistas) {
}
