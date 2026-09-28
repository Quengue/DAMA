package br.com.dama.intelligence.colaborador.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** RF02 — consultar e editar dados de colaboradores. */
public record UpdateColaboradorCommand(
        @NotNull(message = "departamentoId é obrigatório") Long departamentoId,
        @NotBlank(message = "nome é obrigatório") String nome,
        @NotBlank(message = "email é obrigatório") @Email(message = "email inválido") String email,
        String cargo,
        String senioridade
) {
}
