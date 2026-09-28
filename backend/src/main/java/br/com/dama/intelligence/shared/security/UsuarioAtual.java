package br.com.dama.intelligence.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Id do colaborador autenticado na requisição atual (ver ColaboradorHeaderAuthenticationFilter). */
public final class UsuarioAtual {

    private UsuarioAtual() {
    }

    /** Retorna o id do colaborador autenticado, ou null fora de uma requisição autenticada. */
    public static Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long id) {
            return id;
        }
        return null;
    }
}
