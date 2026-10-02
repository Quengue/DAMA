# ADR-0004: Serviço Node.js de alertas acessado pela interface `AlertClient`

- **Status:** Aceita (aplica-se aos projetos de aula)
- **Data:** 2026-09-18

## Contexto

No projeto em camadas, cada registro de consumo de tokens precisava gerar um alerta (INFO abaixo de 5.000 tokens, WARNING até 9.999, CRITICAL a partir de 10.000). A disciplina pedia integração com um serviço Node.js.

## Decisão

O alerta é calculado por um serviço Node.js separado (`alert-service`). O `UsageService` depende só da interface `AlertClient`; a implementação `NodeAlertClient` faz a chamada REST.

## Alternativas consideradas

- Calcular o alerta no próprio Spring Boot — não atenderia a integração pedida.
- Mensageria assíncrona — mais infraestrutura do que o caso exigia.

## Consequências

- Trocar o Node por outro mecanismo exige só outra implementação de `AlertClient`.
- O registro de consumo passa a depender da disponibilidade do serviço Node (chamada síncrona).
- No produto DAMA o mesmo padrão (interface no backend, serviço Node separado) é aplicado aos alertas de gestão, agora com fallback local ([ADR-0009](0009-alert-service-node-para-alertas-de-gestao.md)); o BFF do front-end é tratado no [ADR-0008](0008-front-spa-com-bff-node.md).
