package br.edu.exemplo.ia.usage.internal; import br.edu.exemplo.ia.project.api.ProjectFacade; import br.edu.exemplo.ia.usage.api.*; import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*; import static org.mockito.Mockito.*; import java.util.UUID;
class UsageServiceTest { @Test void moduloConsumoSoConheceApiDoModuloProjeto_AAA(){ // Arrange
 var repo=mock(UsageRepository.class); var projects=mock(ProjectFacade.class); var id=UUID.randomUUID(); when(projects.exists(id)).thenReturn(true); when(repo.save(any())).thenAnswer(i->i.getArgument(0)); var service=new UsageService(repo,projects); // Act
 var result=service.register(new RegisterUsageCommand(id,100,"gpt-demo")); // Assert
 assertEquals(100,result.tokens()); verify(projects).exists(id); } }
