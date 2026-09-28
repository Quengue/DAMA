package br.com.dama.intelligence.conquista.api;

import java.math.BigDecimal;

public record NivelEvolucaoView(Long nivelId, String nome, BigDecimal pontosMinimos, int ordem, String descricao) {
}
