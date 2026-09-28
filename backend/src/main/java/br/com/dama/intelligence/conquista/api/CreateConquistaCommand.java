package br.com.dama.intelligence.conquista.api;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/** criterio: um dos valores de CriterioConquista. valorMinimo é obrigatório nos critérios automáticos e proibido em MANUAL. */
public record CreateConquistaCommand(
        @NotBlank(message = "nome é obrigatório") String nome,
        String descricao,
        @NotBlank(message = "criterio é obrigatório") String criterio,
        BigDecimal valorMinimo
) {
}
