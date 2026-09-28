package br.com.dama.intelligence.indicador.api;

import jakarta.validation.constraints.NotBlank;

/** RF03 — registrar indicadores de desempenho (definição do indicador). RNF08 — nome/unidade claros. */
public record CreateIndicadorCommand(
        @NotBlank(message = "nome é obrigatório") String nome,
        String descricao,
        String unidade
) {
}
