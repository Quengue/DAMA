# Projeto 3 — Microservices

O módulo `project` virou `project-service` (8081) e o módulo `usage` virou `usage-service` (8082). Cada serviço tem **seu próprio código, JAR, imagem e PostgreSQL**. O front Node atua também como BFF simples, roteando `/api/projects` e `/api/usages`.

## O ponto didático central
Antes: `UsageService -> ProjectFacade` era uma chamada Java em memória. Agora: `UsageService -> ProjectClient -> HTTP -> project-service`. A assinatura conceitual continua parecida, mas surgem latência, timeout, indisponibilidade, versionamento de contrato, observabilidade e consistência distribuída.

## Executar
Na raiz: `docker compose up --build`

Front/BFF: http://localhost:3000 | project-service: :8081 | usage-service: :8082

## Deploy
Cada serviço pode ser versionado e implantado independentemente. Isso é ganho real, mas exige infraestrutura e operação distribuídas. O Compose representa um ambiente didático/local; em produção normalmente entram registry, secrets, health/readiness, observabilidade, gateway/ingress e orquestração.
