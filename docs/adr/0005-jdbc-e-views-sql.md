# ADR-0005: Persistência com JdbcTemplate e views SQL, sem JPA

- **Status:** Aceita
- **Data:** 2026-09-28

## Contexto

Boa parte do DAMA é leitura agregada: rankings, visão consolidada, dashboards, saldos de AI Credits. O RNF04 pede que esses números sempre reflitam os dados de origem.

## Decisão

- O schema é mantido em scripts SQL versionados (`database/01_schema.sql`, `02_views.sql`, `03_seed.sql`).
- Os repositórios usam `JdbcTemplate` com SQL explícito.
- Agregações ficam em views (`vw_ranking_colaborador`, `vw_visao_consolidada_empresa`...) ou em consultas do próprio repositório; saldos e totais nunca são armazenados.

## Alternativas consideradas

- JPA/Hibernate — gera o schema implicitamente e esconde as consultas agregadas, que são o centro do produto.
- Tabelas de totais atualizadas por gatilho — risco de divergir da origem.

## Consequências

- O SQL é visível e revisável; o time exercita modelagem relacional.
- Mapeamento de linhas é manual.
- Joins em views agregadas exigem cuidado para não multiplicar somas (bug corrigido em `vw_visao_consolidada_empresa`, coberto pelo teste de integração).
