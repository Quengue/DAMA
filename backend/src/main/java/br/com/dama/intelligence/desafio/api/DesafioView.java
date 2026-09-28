package br.com.dama.intelligence.desafio.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DesafioView(Long desafioId, Long empresaId, String nome, String descricao, LocalDate dataInicio,
                          LocalDate dataFim, BigDecimal pontosRecompensa, String status, long totalParticipantes) {
}
