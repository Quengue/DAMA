package br.edu.exemplo.ia.project.internal;
import jakarta.persistence.Embeddable;
@Embeddable public record ProjectName(String value) { public ProjectName { if(value==null || value.isBlank()) throw new IllegalArgumentException("Nome obrigatório"); } }
