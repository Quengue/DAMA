package br.com.dama.intelligence.aicredit.api;

import java.math.BigDecimal;

public record NivelAutonomiaView(Long nivelId, String nome, BigDecimal creditosMinimos, int ordem, String descricao) {
}
