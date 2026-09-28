package br.com.dama.intelligence.shared.error;

/** Conflito de estado (ex.: e-mail de colaborador já cadastrado na empresa) -> HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
