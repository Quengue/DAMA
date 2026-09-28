package br.com.dama.intelligence.perfil.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreatePerfilCommand(
        @NotBlank(message = "nome é obrigatório") String nome,
        String descricao,
        @NotEmpty(message = "informe ao menos um código de permissão") List<String> permissoes
) {
}
