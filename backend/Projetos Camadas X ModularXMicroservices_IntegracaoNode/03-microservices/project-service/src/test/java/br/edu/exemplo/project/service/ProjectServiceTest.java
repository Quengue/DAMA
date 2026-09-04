package br.edu.exemplo.project.service;

import br.edu.exemplo.project.domain.*;
import br.edu.exemplo.project.dto.ProjectDtos.*;
import br.edu.exemplo.project.repository.ProjectRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectServiceTest {
    @Test
    void criaProjeto_AAA() {// Arrange
        var r = mock(ProjectRepository.class);
        when(r.save(any())).thenAnswer(i -> i.getArgument(0));
        var s = new ProjectService(r);// Act
        var out = s.create(new Request("Tutor IA", "Educação"));// Assert
        assertEquals("Tutor IA", out.name());
    }
}