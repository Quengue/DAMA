package br.com.dama.intelligence.conquista.internal;

import br.com.dama.intelligence.shared.event.DesempenhoAtualizadoEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Reavalia as conquistas automáticas do colaborador na mesma transação da movimentação que disparou o evento:
 * se a concessão falhar, a movimentação também é desfeita.
 */
@Component
class ConquistaEventListener {

    private final ConquistaService service;

    ConquistaEventListener(ConquistaService service) {
        this.service = service;
    }

    @EventListener
    void aoAtualizarDesempenho(DesempenhoAtualizadoEvent evento) {
        service.avaliarColaborador(evento.colaboradorId());
    }
}
