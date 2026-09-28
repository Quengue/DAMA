# ADR-0002: Evoluir para monólito modular

- **Status:** Aceita
- **Data:** 2026-09-28

## Contexto

O documento de requisitos do DAMA tem áreas bem separadas (colaboradores, desempenho, gamificação, AI Credits, analytics). No monólito em camadas ([ADR-0001](0001-monolito-em-camadas.md)) essas áreas ficariam misturadas nas mesmas camadas. O RNF05 pede arquitetura modular e manutenível.

## Decisão

O backend do produto é um monólito modular: um pacote por área de negócio (`colaborador`, `meta`, `aicredit`, `conquista`...), cada um com `api` (Facade, Views, Commands, Controller) e `internal` (Service e Repository package-private). Módulos só conversam pela Facade de outro módulo ou por evento de domínio.

## Alternativas consideradas

- Manter camadas — acoplamento crescente, como descrito no ADR-0001.
- Microservices — ver [ADR-0003](0003-microservices-como-estudo.md).

## Consequências

- Cada área pode evoluir sem conhecer os detalhes internos das outras; o compilador impede acesso a classes `internal`.
- Um único deploy e um único banco continuam simples de operar.
- Se um módulo precisar escalar sozinho no futuro, a Facade já é a fronteira natural para extraí-lo como serviço.
