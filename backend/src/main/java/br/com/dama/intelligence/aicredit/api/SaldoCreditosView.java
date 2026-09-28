package br.com.dama.intelligence.aicredit.api;

import java.math.BigDecimal;

/** RF23 (saldo) + RF26 (nível de autonomia atual, nulo se o saldo ainda não atingiu nenhum nível). */
public record SaldoCreditosView(Long colaboradorId, BigDecimal saldoAtual, NivelAutonomiaView nivelAtual) {
}
