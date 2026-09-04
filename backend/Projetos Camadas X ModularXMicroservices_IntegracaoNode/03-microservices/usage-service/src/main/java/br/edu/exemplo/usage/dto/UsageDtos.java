package br.edu.exemplo.usage.dto;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.UUID;

public final class UsageDtos {
    private UsageDtos() {
    }

    public record Request(@NotNull UUID projectId, @Positive long tokens, @NotBlank String model) {
    }

    public record Response(UUID id, UUID projectId, long tokens, String model, Instant occurredAt) {
    }
}