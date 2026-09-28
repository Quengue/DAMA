package br.com.dama.intelligence.perfil.api;

import java.util.List;

public record PerfilView(Long perfilId, String nome, String descricao, List<String> permissoes) {
}
