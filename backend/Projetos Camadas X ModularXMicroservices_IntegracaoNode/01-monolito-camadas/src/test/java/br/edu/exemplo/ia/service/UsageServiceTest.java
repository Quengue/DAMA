package br.edu.exemplo.ia.service;
import br.edu.exemplo.ia.domain.entity.AIUsage;
import br.edu.exemplo.ia.dto.*;
import br.edu.exemplo.ia.repository.UsageRepository;
import org.junit.jupiter.api.*;
import org.mockito.*; import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsageServiceTest {
 @Test void deveRegistrarConsumo_AAA(){
  // Arrange
  UsageRepository repo=mock(UsageRepository.class); ProjectUseCase projects=mock(ProjectUseCase.class); UUID pid=UUID.randomUUID(); when(projects.exists(pid)).thenReturn(true); when(repo.save(any())).thenAnswer(i->i.getArgument(0)); UsageService service=new UsageService(repo,projects);
  // Act
  UsageResponse result=service.register(new UsageRequest(pid,500,"gpt-demo"));
  // Assert
  assertEquals(500,result.tokens()); verify(repo).save(any(AIUsage.class));
 }
}
