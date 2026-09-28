package br.edu.exemplo.usage.integration.alert;

import java.util.UUID;

public record AlertRequest(UUID usageId, UUID projectId, long tokens, String model) {
}
