package br.com.dama.intelligence.perfil.api;

import jakarta.validation.constraints.NotNull;

public record AtribuirPerfilCommand(@NotNull(message = "perfilId é obrigatório") Long perfilId) {
}
