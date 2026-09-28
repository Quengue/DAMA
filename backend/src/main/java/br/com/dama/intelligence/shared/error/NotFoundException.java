package br.com.dama.intelligence.shared.error;

/** Reaproveitada do módulo anterior: recurso inexistente -> HTTP 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
