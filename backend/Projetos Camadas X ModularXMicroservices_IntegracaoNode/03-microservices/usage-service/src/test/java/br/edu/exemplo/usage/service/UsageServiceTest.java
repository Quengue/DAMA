package br.edu.exemplo.usage.service;

import br.edu.exemplo.usage.dto.UsageDtos.Request;
import br.edu.exemplo.usage.integration.ProjectClient;
import br.edu.exemplo.usage.repository.UsageRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsageServiceTest {
    @Test
    void registraViaContratoRemoto_AAA() {// Arrange
        var repo = mock(UsageRepository.class);
        var client = mock(ProjectClient.class);
        var id = UUID.randomUUID();
        when(client.exists(id)).thenReturn(true);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        var s = new UsageService(repo, client);// Act
        var out = s.register(new Request(id, 200, "gpt-demo"));// Assert
        assertEquals(200, out.tokens());
        verify(client).exists(id);
    }
}