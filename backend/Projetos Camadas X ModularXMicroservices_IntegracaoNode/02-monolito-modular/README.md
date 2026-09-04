# Projeto 2 — Monólito Modular

Mesmo comportamento externo, mas organização **vertical por domínio**: `project` e `usage`. Cada módulo tem `api` pública e `internal` escondido por convenção/package visibility.

## Diferença essencial
`usage.internal.UsageService` **não importa Repository, Entity ou Service interno de project**. Ele depende somente de `project.api.ProjectFacade`. Isso cria uma fronteira que pode virar REST/evento no futuro com menor impacto.

Ainda existe: **um processo, um JAR, uma imagem e um PostgreSQL**. Portanto modularidade não é microservice; é preparação estrutural.

## Executar
`docker compose up --build`

Front: http://localhost:3000 | Backend: http://localhost:8080

## Deploy
O deployment continua atômico: todos os módulos são publicados juntos. A vantagem aparece na organização, testes, ownership e facilidade de extração futura.
