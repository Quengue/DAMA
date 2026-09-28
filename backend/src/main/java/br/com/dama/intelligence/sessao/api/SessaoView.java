package br.com.dama.intelligence.sessao.api;

import br.com.dama.intelligence.colaborador.api.ColaboradorView;

import java.util.List;

/** Quem está autenticado e o que pode fazer: usado pelo front para montar menu e telas. */
public record SessaoView(ColaboradorView colaborador, List<String> perfis, List<String> permissoes) {
}
