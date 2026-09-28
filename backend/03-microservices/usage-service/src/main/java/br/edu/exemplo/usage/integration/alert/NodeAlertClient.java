package br.edu.exemplo.usage.integration.alert;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class NodeAlertClient implements AlertClient {
    private final RestClient client;

    public NodeAlertClient(@Value("${alert-service.base-url}") String url) {
        this.client = RestClient.builder().baseUrl(url).build();
    }

    @Override
    public AlertResponse evaluate(UUID usageId, UUID projectId, long tokens, String model) {
        return client.post()
                .uri("/api/alerts/evaluate")
                .body(new AlertRequest(usageId, projectId, tokens, model))
                .retrieve()
                .body(AlertResponse.class);
    }
}
