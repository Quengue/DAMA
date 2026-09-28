package br.com.dama.intelligence.desafio.api;

import java.math.BigDecimal;

public record ParticipanteView(Long desafioId, Long colaboradorId, String status, BigDecimal pontosObtidos) {
}
