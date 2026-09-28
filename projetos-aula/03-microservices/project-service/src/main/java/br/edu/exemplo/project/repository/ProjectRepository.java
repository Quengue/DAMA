package br.edu.exemplo.project.repository;

import br.edu.exemplo.project.domain.AIProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProjectRepository extends JpaRepository<AIProject, UUID> {
}