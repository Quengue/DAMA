package br.com.dama.intelligence.shared.error;

/** Regra de negócio violada (ex.: meta sem colaborador nem departamento) -> HTTP 400. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
