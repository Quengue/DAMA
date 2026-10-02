# ADR-0009: Alertas de gestão calculados por um serviço Node.js (`alert-service`)

- **Status:** Aceita
- **Data:** 2026-10-02

## Contexto

As análises de gestão (RF15) apontam metas vencidas ou em risco, indicadores em queda, equipes abaixo da média e colaboradores sem atividade. Até a versão 0.2 as regras ficavam só no backend (`AnaliseGestaoRegras`). O front já passa por um BFF Node ([ADR-0008](0008-front-spa-com-bff-node.md)), mas o backend do produto não integrava nenhum serviço Node, ao contrário dos projetos de aula ([ADR-0004](0004-alert-service-node-via-alertclient.md)). A disciplina de Programação Backend pede essa integração, e o feedback da banca pediu atenção ao backend e às integrações.

## Decisão

- Um serviço Node.js separado, `alert-service/`, recebe por `POST /avaliar` os dados brutos lidos do banco (metas abertas, últimos dois valores de cada série de indicador, pontos por equipe, colaboradores sem atividade) e devolve a lista de alertas.
- O backend lê o banco e continua dono da ordenação por severidade, da contagem e da autorização. O `AnalyticsService` depende apenas da interface `AlertaGestaoClient`.
- Duas implementações: `AlertaGestaoNode` (chamada REST síncrona, timeout de 2 s) e `AlertaGestaoLocal` (as regras em Java). A escolha vem de `ALERTAS_URL`: com a variável, usa o Node; sem ela, usa só as regras locais.
- Se o Node estiver fora do ar, lento ou devolver uma resposta fora do contrato, o backend registra um aviso no log e usa as regras locais. A tela de análises não quebra.

## Alternativas consideradas

- Manter tudo no backend — não atenderia à integração pedida pela disciplina.
- Mover as regras só para o Node, sem fallback — o endpoint de alertas ficaria indisponível junto com o serviço, o ponto fraco já registrado no ADR-0004.
- Mensageria assíncrona — mais infraestrutura (broker) do que a análise sob demanda exige.
- Fazer o BFF do front calcular os alertas — o BFF só encaminha chamadas e não tem acesso ao banco.

## Consequências

- O contrato de entrada e saída está documentado em [`alert-service/README.md`](../../alert-service/README.md) e coberto por testes dos dois lados (`alert-service/test` e `AlertaGestaoNodeTest`).
- As regras existem em duas linguagens. Qualquer mudança de regra precisa ser feita nas duas ou o fallback passa a se comportar diferente do serviço. Os testes dos dois lados usam os mesmos casos para reduzir esse risco.
- A análise de gestão agora faz uma chamada de rede. O timeout e o fallback limitam o custo, e o log mostra qual motor respondeu.
- Não é uma migração para microservices ([ADR-0003](0003-microservices-como-estudo.md)): o serviço não tem banco próprio, não guarda estado e o produto continua funcionando sem ele.
