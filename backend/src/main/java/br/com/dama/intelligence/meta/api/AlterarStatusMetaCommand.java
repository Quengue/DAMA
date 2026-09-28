package br.com.dama.intelligence.meta.api;

import jakarta.validation.constraints.NotBlank;

/** Valores aceitos: Em andamento, Concluida, Atrasada, Cancelada. */
public record AlterarStatusMetaCommand(@NotBlank(message = "status é obrigatório") String status) {
}
