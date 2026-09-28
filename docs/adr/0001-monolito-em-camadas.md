# ADR-0001: Primeira versão como monólito em camadas

- **Status:** Substituída por [ADR-0002](0002-monolito-modular.md)
- **Data:** 2026-09-04

## Contexto

A primeira entrega precisava de uma API funcionando rápido, com o domínio de exemplo da disciplina (projetos de IA e consumo de tokens) e com um time aprendendo Spring Boot.

## Decisão

Organizar o backend horizontalmente em `controller → service → repository → entity`, com JPA e um único deploy. Código em [`projetos-aula/01-monolito-camadas`](../../projetos-aula/01-monolito-camadas).

## Alternativas consideradas

- Monólito modular desde o início — exigia definir fronteiras de módulo antes de conhecer o domínio.
- Microservices — custo operacional alto para um time pequeno e um domínio ainda instável.

## Consequências

- Simples de entender e de subir.
- Qualquer classe pode depender de qualquer outra: à medida que o domínio cresce, mudanças em uma funcionalidade espalham-se por todas as camadas.
