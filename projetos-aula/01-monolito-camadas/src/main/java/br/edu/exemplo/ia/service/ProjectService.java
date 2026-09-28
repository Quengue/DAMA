package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.domain.entity.AIProject;
import br.edu.exemplo.ia.domain.vo.ProjectName;
import br.edu.exemplo.ia.dto.ProjectRequest;
import br.edu.exemplo.ia.dto.ProjectResponse;
import br.edu.exemplo.ia.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService implements ProjectUseCase {

    private final ProjectRepository repo;

    public ProjectService(ProjectRepository repo) {
        this.repo = repo;
    }

    @Override
    public ProjectResponse create(ProjectRequest r) {

        AIProject project = new AIProject(
                new ProjectName(r.name()),
                r.area()
        );

        return toDto(repo.save(project));
    }

    @Override
    public List<ProjectResponse> list() {

        return repo.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public boolean exists(UUID id) {
        return repo.existsById(id);
    }

    private ProjectResponse toDto(AIProject p) {

        return new ProjectResponse(
                p.getId(),
                p.getName().value(),
                p.getArea()
        );
    }
}