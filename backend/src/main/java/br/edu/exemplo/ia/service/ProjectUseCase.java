package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.dto.ProjectRequest;
import br.edu.exemplo.ia.dto.ProjectResponse;

import java.util.List;
import java.util.UUID;

public interface ProjectUseCase {

    ProjectResponse create(ProjectRequest r);

    List<ProjectResponse> list();

    boolean exists(UUID id);
}