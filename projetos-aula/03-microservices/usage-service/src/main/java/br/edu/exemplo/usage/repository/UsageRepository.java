package br.edu.exemplo.usage.repository;

import br.edu.exemplo.usage.domain.AIUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UsageRepository extends JpaRepository<AIUsage, UUID> {
}