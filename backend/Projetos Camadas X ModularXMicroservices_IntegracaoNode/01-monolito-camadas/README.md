# Projeto 1 — Monólito em Camadas

Mesmo domínio da trilha: projetos de IA e registro de consumo de tokens. Organização **horizontal**: `controller -> service -> repository -> entity`.

## Pontos didáticos
- DDD leve: `ProjectName` é Value Object; entidades possuem identidade; DTOs isolam a API.
- IoC/DI via injeção por construtor e interfaces `ProjectUseCase` / `UsageUseCase`.
- O código é desacoplado tecnicamente, mas `UsageService` depende do caso de uso de Projeto e ambos compartilham o mesmo banco/deployment.
- Testes: Mockito (unidade) e Testcontainers/PostgreSQL (integração), com comentários AAA.

## Executar
`docker compose up --build`

Front: http://localhost:3000  | Backend: http://localhost:8080 | Health: http://localhost:8080/actuator/health

## Deploy explicado
1. Maven produz **um JAR**.
2. Docker produz **uma imagem do backend**.
3. Compose sobe frontend + backend + **um PostgreSQL**.
4. Alteração em qualquer funcionalidade exige novo deploy do monólito inteiro.
