package br.edu.exemplo.ia.usage.internal;
import br.edu.exemplo.ia.project.api.ProjectFacade;
import br.edu.exemplo.ia.usage.api.*;
import org.springframework.stereotype.Service;

@Service public class UsageService implements UsageFacade {
    private final UsageRepository repo;
    private final ProjectFacade projects;
    public UsageService(UsageRepository repo,ProjectFacade projects){
        this.repo=repo;this.projects=projects;
    }

    public UsageView register(RegisterUsageCommand c){
        if(!projects.exists(c.projectId()))throw new IllegalArgumentException("Projeto inexistente");
        var u=repo.save(new AIUsage(c.projectId(),c.tokens(),c.model()));
        return new UsageView(u.getId(),u.getProjectId(),u.getTokens(),u.getModel(),u.getOccurredAt());
    }
}
