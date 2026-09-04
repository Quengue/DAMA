package br.edu.exemplo.usage;

import br.edu.exemplo.usage.domain.AIUsage;
import br.edu.exemplo.usage.repository.UsageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
class UsageRepositoryIntegrationTest {
    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void p(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
    }

    @Autowired
    UsageRepository repo;

    @Test
    void persisteNoPostgres_AAA() {// Arrange
        var u = new AIUsage(UUID.randomUUID(), 300, "gpt-demo");// Act
        var saved = repo.saveAndFlush(u);// Assert
        assertNotNull(saved.getId());
        assertEquals(300, saved.getTokens());
    }
}