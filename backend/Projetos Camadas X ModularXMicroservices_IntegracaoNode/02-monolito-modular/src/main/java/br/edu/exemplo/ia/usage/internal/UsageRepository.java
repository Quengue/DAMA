package br.edu.exemplo.ia.usage.internal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

interface UsageRepository extends JpaRepository<AIUsage,UUID>{

}