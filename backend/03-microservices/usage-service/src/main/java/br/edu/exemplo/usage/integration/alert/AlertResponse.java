package br.edu.exemplo.usage.integration.alert;

import java.time.Instant;
import java.util.UUID;

public record AlertResponse(UUID id, UUID usageId, UUID projectId, String level, String message, Instant createdAt) {
}
