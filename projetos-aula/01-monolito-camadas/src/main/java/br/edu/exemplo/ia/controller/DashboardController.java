package br.edu.exemplo.ia.controller;

import br.edu.exemplo.ia.dto.DashboardResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @GetMapping
    public DashboardResponse dashboard() {
        return new DashboardResponse(
                2356,
                18,
                161,
                269515
        );
    }
}