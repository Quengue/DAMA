package br.edu.exemplo.ia.project.api;
import java.util.*;
public interface ProjectFacade {
    ProjectView create(CreateProjectCommand c);
    List<ProjectView> list(); boolean exists(UUID id);
}