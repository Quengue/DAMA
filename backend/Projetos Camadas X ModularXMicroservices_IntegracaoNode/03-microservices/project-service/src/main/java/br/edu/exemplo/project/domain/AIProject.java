package br.edu.exemplo.project.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "ai_project")
public class AIProject {
    @Id
    private UUID id;
    @Embedded
    private ProjectName name;
    private String area;

    protected AIProject() {
    }

    public AIProject(ProjectName n, String a) {
        id = UUID.randomUUID();
        name = n;
        area = a;
    }

    public UUID getId() {
        return id;
    }

    public ProjectName getName() {
        return name;
    }

    public String getArea() {
        return area;
    }
}