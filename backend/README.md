# Backend — API REST

Spring Boot 3.3, Java 17, PostgreSQL 16 via JdbcTemplate. Monólito modular: cada pasta em `src/main/java/br/com/dama/intelligence` é um módulo com `api` (contrato público) e `internal` (implementação).

| Módulo | Responsabilidade |
|---|---|
| `organizacao` | Empresas e equipes (departamentos) |
| `colaborador` | Cadastro, edição e desligamento de colaboradores |
| `perfil` | Perfis e permissões |
| `sessao` | `GET /api/me` |
| `indicador` | Indicadores e série histórica de valores |
| `meta` | Metas, progresso e conclusão automática |
| `pontuacao` | Regras de pontuação e lançamentos |
| `desafio` | Desafios e participantes |
| `reconhecimento` | Reconhecimentos |
| `conquista` | Conquistas e níveis de evolução |
| `aicredit` | Atividades produtivas, regras de crédito, saldo, histórico e níveis de autonomia |
| `dashboard` | Dashboard do colaborador e da equipe |
| `analytics` | Rankings, visão consolidada, comparação entre equipes e análises de gestão |
| `auditoria` | Registro das movimentações |
| `shared` | Segurança, tratamento de erros, evento de domínio e relógio |

Diagrama de componentes: [docs/arquitetura/c4-3-componentes-backend.md](../docs/arquitetura/c4-3-componentes-backend.md). Endpoints: [docs/api](../docs/api/).

## Rodar

```bash
mvn spring-boot:run
```

Variáveis: `DB_HOST` (localhost), `DB_PORT` (5432), `DB_NAME`, `DB_USER`, `DB_PASSWORD` (dama). O banco precisa dos scripts de [`../database`](../database).

## Testes

```bash
mvn test
```

- `analytics/internal/AnaliseGestaoRegrasTest`, `conquista/internal/ConquistaServiceTest`, `meta/internal/MetaServiceTest`: regras de negócio, sem banco.
- `DamaApiIntegrationTest`: API inteira contra PostgreSQL 16 (Testcontainers) com os scripts de `database/`. Pulado se não houver Docker.

## Imagem

```bash
docker build -t dama-backend .
```
