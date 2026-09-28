# ADR-0003: Microservices mantidos como estudo, não como arquitetura do produto

- **Status:** Aceita
- **Data:** 2026-09-28

## Contexto

Na disciplina, o domínio de exemplo foi separado em `project-service` e `usage-service`, com comunicação REST síncrona ([`projetos-aula/03-microservices`](../../projetos-aula/03-microservices)). A dúvida era se o DAMA deveria seguir o mesmo caminho.

## Decisão

O produto não usa microservices agora. A versão em microservices fica em `projetos-aula/` como referência da evolução estudada.

## Alternativas consideradas

- Separar o DAMA em serviços (desempenho, gamificação, AI Credits) — as regras cruzam áreas o tempo todo (metas geram pontos, pontos geram conquistas, atividades geram créditos). Separar exigiria transações distribuídas ou consistência eventual sem ganho de escala que justifique.

## Consequências

- Menos infraestrutura (um backend, um banco).
- A modularização do [ADR-0002](0002-monolito-modular.md) preserva a opção de extrair serviços depois.
