package br.com.dama.intelligence.shared.event;

/**
 * Publicado (via ApplicationEventPublisher) sempre que algo que conta para conquistas muda para um
 * colaborador: pontos, metas concluídas, desafios, reconhecimentos, atividades ou AI Credits.
 * O módulo conquista escuta este evento; quem publica não conhece quem escuta.
 */
public record DesempenhoAtualizadoEvent(Long colaboradorId) {
}
