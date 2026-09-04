package br.edu.exemplo.project.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public final class ProjectDtos {
    private ProjectDtos() {
    }

    public record Request(@NotBlank String name, @NotBlank String area) {
    }

    public record Response(UUID id, String name, String area) {
    }
}