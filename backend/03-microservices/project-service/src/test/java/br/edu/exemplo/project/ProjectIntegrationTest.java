package br.edu.exemplo.project;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProjectIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveResponderAoListarProjetos() throws Exception {

        // Arrange
        String endpoint = "/api/projects";

        // Act
        ResultActions resultado =
                mockMvc.perform(get(endpoint));

        // Assert
        resultado.andExpect(status().isOk());
    }
}