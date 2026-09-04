package br.edu.exemplo.project.service;

import br.edu.exemplo.project.domain.*;
import br.edu.exemplo.project.dto.ProjectDtos.*;
import br.edu.exemplo.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ProjectService {
    private final ProjectRepository repo;

    public ProjectService(ProjectRepository r) {
        repo = r;
    }

    public Response create(Request r) {
        return map(repo.save(new AIProject(new ProjectName(r.name()), r.area())));
    }

    public List<Response> list() {
        return repo.findAll().stream().map(this::map).toList();
    }

    public boolean exists(UUID id) {
        return repo.existsById(id);
    }

    private Response map(AIProject p) {
        return new Response(p.getId(), p.getName().value(), p.getArea());
    }
}