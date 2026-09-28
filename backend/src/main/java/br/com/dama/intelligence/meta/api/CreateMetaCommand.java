package br.com.dama.intelligence.meta.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** RF04 — definir meta. RN05: a meta deve estar associada a um colaborador ou a uma equipe (departamento). */
public record CreateMetaCommand(
        Long indicadorId,
        Long colaboradorId,
        Long departamentoId,
        @NotBlank(message = "descricao é obrigatória") String descricao,
        @NotNull(message = "valorAlvo é obrigatório") BigDecimal valorAlvo,
        @NotNull(message = "dataInicio é obrigatória") LocalDate dataInicio,
        @NotNull(message = "dataFim é obrigatória") LocalDate dataFim
) {
}
