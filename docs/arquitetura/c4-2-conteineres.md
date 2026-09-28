# C4 — Nível 2: Contêineres

```mermaid
flowchart LR
    usuario["Usuário<br/><small>[Pessoa]</small>"]

    subgraph dama["DAMA Intelligence"]
        direction LR
        front["Front-end + BFF<br/><small>[Node 22 / Express 5]</small><br/>Serve a SPA (JS puro + Chart.js)<br/>e encaminha /api/* à API"]
        api["API REST<br/><small>[Java 17 / Spring Boot 3]</small><br/>Monólito modular: regras de negócio,<br/>autorização por perfil, auditoria"]
        db[("Banco de dados<br/><small>[PostgreSQL 16]</small><br/>Schema dama: tabelas,<br/>views de ranking e dashboards")]
    end

    usuario -- "HTTPS :3000" --> front
    front -- "REST/JSON :8080<br/>cabeçalho X-Colaborador-Id" --> api
    api -- "JDBC" --> db

    classDef conteiner fill:#1a1a1a,stroke:#ff6a00,color:#f2f2f2
    classDef banco fill:#131313,stroke:#5aa9ff,color:#f2f2f2
    class front,api conteiner
    class db banco
```

| Contêiner | Pasta | Imagem / porta | Saúde |
|---|---|---|---|
| Front-end + BFF | `frontend/` | `node:22-alpine`, 3000 | `GET /health` (verifica também a API) |
| API | `backend/` | `eclipse-temurin:17-jre`, 8080 | `GET /actuator/health` |
| Banco | `database/` (scripts) | `postgres:16`, 5432 | `pg_isready` |

O navegador nunca fala direto com a API: tudo passa pelo BFF, que preserva o caminho `/api/*` e o cabeçalho de identificação do usuário.
