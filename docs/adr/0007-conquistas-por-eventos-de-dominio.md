# ADR-0007: Conquistas avaliadas por evento de domínio

- **Status:** Aceita
- **Data:** 2026-09-28

## Contexto

Uma conquista pode depender de pontos, metas concluídas, desafios, reconhecimentos, atividades registradas ou AI Credits (RN18). Se cada um desses módulos chamasse o módulo `conquista`, todos passariam a depender dele.

## Decisão

Os módulos publicam `DesempenhoAtualizadoEvent(colaboradorId)` pelo `ApplicationEventPublisher` do Spring. O módulo `conquista` escuta o evento e, na mesma transação, concede de uma vez todas as conquistas automáticas cujo critério o colaborador já cumpre (um único `INSERT ... SELECT ... ON CONFLICT DO NOTHING`).

## Alternativas consideradas

- Chamada direta à `ConquistaFacade` — acoplaria cinco módulos ao de conquistas.
- Avaliar só na leitura (ao abrir o perfil) — a data de obtenção deixaria de ser a data real.
- Processamento assíncrono (fila ou `@TransactionalEventListener` após o commit) — mais peças para um volume que não pede isso.

## Consequências

- Novos critérios só alteram o módulo `conquista`.
- A concessão é atômica com a movimentação que a gerou; se falhar, a movimentação também é desfeita.
- Conquistas criadas depois são avaliadas retroativamente para a empresa inteira, e há `POST /api/empresas/{id}/conquistas/reavaliar` para reprocessar.
