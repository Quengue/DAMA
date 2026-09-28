# Banco de dados

PostgreSQL 16, schema `dama`. Os scripts rodam em ordem na criação do contêiner (`docker-entrypoint-initdb.d`).

| Arquivo | Conteúdo |
|---|---|
| `01_schema.sql` | Tabelas e índices. Recria o schema do zero (`DROP SCHEMA dama CASCADE`) |
| `02_views.sql` | Views de ranking, visão consolidada, indicadores por equipe e saldo de AI Credits. Pode ser reexecutado |
| `03_seed.sql` | Perfis e permissões, empresa de demonstração, administrador (id 1), níveis de autonomia e de evolução |
| `demo/04_demo.sql` | Opcional: equipes, 13 colaboradores, 6 meses de indicadores, metas, pontos, desafios, AI Credits e conquistas. Datas relativas ao dia em que roda |

Aplicar manualmente:

```bash
psql -U dama -d dama -f 01_schema.sql -f 02_views.sql -f 03_seed.sql -f demo/04_demo.sql
```
