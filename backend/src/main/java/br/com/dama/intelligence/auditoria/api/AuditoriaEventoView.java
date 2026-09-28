package br.com.dama.intelligence.auditoria.api;

import java.time.LocalDateTime;

public record AuditoriaEventoView(
        Long eventoId,
        Long colaboradorId,
        String entidade,
        String entidadeId,
        String acao,
        String detalhes,
        LocalDateTime registradoEm
) {
}
