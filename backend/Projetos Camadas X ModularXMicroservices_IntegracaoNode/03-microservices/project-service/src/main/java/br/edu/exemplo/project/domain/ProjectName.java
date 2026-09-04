package br.edu.exemplo.project.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public record ProjectName(String value) {
    public ProjectName {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Nome obrigatório");
    }
}