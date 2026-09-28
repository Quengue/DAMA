package br.com.dama.intelligence.indicador.api;

public record IndicadorView(Long indicadorId, Long empresaId, String nome, String descricao, String unidade,
                            boolean maiorMelhor, boolean ativo) {
}
