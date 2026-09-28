package br.com.dama.intelligence.aicredit.api;

import java.math.BigDecimal;

/** Resultado do registro de uma atividade: se foi elegível (RF21) e quantos AI Credits foram atribuídos (RF22). */
public record RegistroAtividadeResultadoView(AtividadeRegistroView registro, boolean elegivel, BigDecimal creditosConcedidos) {
}
