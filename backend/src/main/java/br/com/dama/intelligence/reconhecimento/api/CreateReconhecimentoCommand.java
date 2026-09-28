package br.com.dama.intelligence.reconhecimento.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** RF10 — reconhecer um colaborador. Quem concede é sempre o usuário autenticado. */
public record CreateReconhecimentoCommand(
        @NotNull(message = "colaboradorId é obrigatório") Long colaboradorId,
        @NotBlank(message = "tipo é obrigatório") String tipo,
        @NotBlank(message = "descricao é obrigatória") String descricao
) {
}
