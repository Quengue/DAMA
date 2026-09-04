package br.edu.exemplo.project.controller;

import br.edu.exemplo.project.dto.ProjectDtos.*;
import br.edu.exemplo.project.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/projects")

public class ProjectController {
    private final ProjectService service;

    public ProjectController(ProjectService s) {
        service = s;
    }

    @PostMapping
    public Response create(@Valid @RequestBody Request r) {
        return service.create(r);
    }

    @GetMapping
    public List<Response> list() {
        return service.list();
    }

    @GetMapping("/{id}/exists")
    public Map<String, Boolean> exists(@PathVariable UUID id) {
        return Map.of("exists", service.exists(id));
    }
}