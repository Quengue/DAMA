package br.edu.exemplo.ia.service;
import br.edu.exemplo.ia.domain.entity.AIUsage; import br.edu.exemplo.ia.dto.*; import br.edu.exemplo.ia.repository.UsageRepository; import org.springframework.stereotype.Service;
@Service public class UsageService implements UsageUseCase {
 private final UsageRepository usageRepo; private final ProjectUseCase projects;
 public UsageService(UsageRepository usageRepo,ProjectUseCase projects){this.usageRepo=usageRepo;this.projects=projects;}
 public UsageResponse register(UsageRequest r){if(!projects.exists(r.projectId()))throw new IllegalArgumentException("Projeto inexistente"); AIUsage u=usageRepo.save(new AIUsage(r.projectId(),r.tokens(),r.model())); return new UsageResponse(u.getId(),u.getProjectId(),u.getTokens(),u.getModel(),u.getOccurredAt());}
}
