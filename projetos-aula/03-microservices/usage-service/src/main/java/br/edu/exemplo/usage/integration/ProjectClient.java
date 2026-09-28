package br.edu.exemplo.usage.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
public class ProjectClient {
    private final RestClient client;

    public ProjectClient(RestClient.Builder b, @Value("${project-service.base-url}") String url) {
        client = b.baseUrl(url).build();
    }

    public boolean exists(UUID id) {
        Map<?, ?> r = client.get().uri("/api/projects/{id}/exists", id).retrieve().body(Map.class);
        return Boolean.TRUE.equals(r.get("exists"));
    }
}