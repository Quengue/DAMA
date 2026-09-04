package br.edu.exemplo.ia.dto;

public record DashboardResponse(
        int employees,
        int companies,
        int departments,
        int aiUsage
) {
}