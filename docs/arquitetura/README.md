# Arquitetura

Diagramas no modelo C4, em Mermaid (renderizam direto no GitHub):

1. [Contexto](c4-1-contexto.md) — o sistema, quem usa e com o que se integra.
2. [Contêineres](c4-2-conteineres.md) — front-end/BFF, API e banco.
3. [Componentes do backend](c4-3-componentes-backend.md) — módulos do monólito modular e suas dependências.

Decisões que levaram a esta arquitetura: [ADRs](../adr/).

## Resumo

- **Front-end**: SPA em JavaScript puro servida por um BFF Node/Express, que também encaminha `/api/*` para a API.
- **Backend**: monólito modular em Spring Boot 3 (Java 17). Cada módulo tem `api` (Facade, Views, Commands, Controller) e `internal` (Service, Repository JDBC). Um módulo só enxerga o pacote `api` de outro.
- **Banco**: PostgreSQL 16, schema `dama`, criado por scripts versionados em `database/`. Dashboards e rankings leem de views; nada é pré-calculado.
- **Execução**: `docker compose up --build` sobe os três contêineres.
