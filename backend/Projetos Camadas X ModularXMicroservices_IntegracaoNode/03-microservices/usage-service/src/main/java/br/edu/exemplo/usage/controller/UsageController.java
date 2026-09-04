package br.edu.exemplo.usage.controller;

import br.edu.exemplo.usage.dto.UsageDtos.Request;
import br.edu.exemplo.usage.dto.UsageDtos.Response;
import br.edu.exemplo.usage.service.UsageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usages")
public class UsageController {
    private final UsageService service;

    public UsageController(UsageService s) {
        service = s;
    }

    @PostMapping
    public Response register(@Valid @RequestBody Request r) {
        return service.register(r);
    }
}