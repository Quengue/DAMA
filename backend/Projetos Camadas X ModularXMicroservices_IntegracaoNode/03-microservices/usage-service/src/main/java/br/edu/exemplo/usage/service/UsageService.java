package br.edu.exemplo.usage.service;

import br.edu.exemplo.usage.domain.AIUsage;
import br.edu.exemplo.usage.dto.UsageDtos.*;
import br.edu.exemplo.usage.integration.ProjectClient;
import br.edu.exemplo.usage.repository.UsageRepository;
import org.springframework.stereotype.Service;

@Service
public class UsageService {
    private final UsageRepository repo;
    private final ProjectClient projects;

    public UsageService(UsageRepository r, ProjectClient p) {
        repo = r;
        projects = p;
    }

    public Response register(Request r) {
        if (!projects.exists(r.projectId()))
            throw new IllegalArgumentException("Projeto inexistente/remoto indisponível");
        var u = repo.save(new AIUsage(r.projectId(), r.tokens(), r.model()));
        return new Response(u.getId(), u.getProjectId(), u.getTokens(), u.getModel(), u.getOccurredAt());
    }
}