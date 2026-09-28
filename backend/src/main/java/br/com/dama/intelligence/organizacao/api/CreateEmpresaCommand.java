package br.com.dama.intelligence.organizacao.api;

import jakarta.validation.constraints.NotBlank;

public record CreateEmpresaCommand(@NotBlank(message = "nome é obrigatório") String nome, String setor, String porte) {
}
