package br.com.dama.intelligence.colaborador.api;

import java.time.LocalDate;

public record ColaboradorView(
        Long colaboradorId,
        Long empresaId,
        Long departamentoId,
        String departamentoNome,
        String nome,
        String email,
        String cargo,
        String senioridade,
        LocalDate dataAdmissao,
        boolean ativo
) {
}
