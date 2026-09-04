package br.edu.exemplo.usage.integration.alert;

import java.util.UUID;

public interface AlertClient {
    AlertResponse evaluate(UUID usageId, UUID projectId, long tokens, String model);
}
