# alert-service

Serviço Node.js que aplica as regras dos alertas de gestão (RF15) do DAMA Intelligence. O backend Spring Boot lê o banco, envia os dados brutos e recebe os alertas. Decisão registrada no [ADR-0009](../docs/adr/0009-alert-service-node-para-alertas-de-gestao.md).

Sem dependências externas: usa só o módulo `http` do Node (>= 20).

## Rodar

```bash
npm start            # porta 4000 (variável PORT)
npm test             # node --test: regras e API HTTP
```

No projeto completo ele sobe com `docker compose up --build`, e o backend recebe `ALERTAS_URL=http://alert-service:4000`.

## API

| Método | Caminho | O que faz |
|---|---|---|
| GET | `/health` | `{ "status": "UP" }`, usado pelo healthcheck do compose |
| POST | `/avaliar` | Recebe os dados da análise e devolve os alertas |

Erros de entrada voltam como `400` (ou `413` para corpo acima de 2 MB) com `{ "status", "message" }`.

### Entrada de `POST /avaliar`

Datas em `AAAA-MM-DD`. Listas ausentes valem como vazias; `dataReferencia` é obrigatória.

```json
{
  "dataReferencia": "2026-09-28",
  "metas": [{
    "metaId": 1, "descricao": "Entregas no prazo",
    "colaboradorId": 10, "colaboradorNome": "Lucas", "departamentoId": null, "departamentoNome": null,
    "valorAlvo": 100, "progressoAtual": 40, "dataInicio": "2026-07-30", "dataFim": "2026-09-27", "maiorMelhor": true
  }],
  "tendencias": [{
    "anterior": { "indicadorId": 5, "indicadorNome": "Produtividade", "unidade": "%", "maiorMelhor": true,
                  "colaboradorId": 10, "colaboradorNome": "Lucas", "departamentoId": null, "departamentoNome": null,
                  "periodo": "2026-08-28", "valor": 80 },
    "atual":    { "indicadorId": 5, "indicadorNome": "Produtividade", "unidade": "%", "maiorMelhor": true,
                  "colaboradorId": 10, "colaboradorNome": "Lucas", "departamentoId": null, "departamentoNome": null,
                  "periodo": "2026-09-28", "valor": 56 }
  }],
  "equipes": [{ "departamentoId": 2, "nome": "Comercial", "colaboradoresAtivos": 4, "pontosTotais": 100 }],
  "colaboradoresSemAtividade": [{ "colaboradorId": 7, "nome": "Marina", "departamentoId": 2 }]
}
```

### Saída

```json
{
  "motor": "alert-service-node",
  "alertas": [{
    "tipo": "META_VENCIDA", "severidade": "ALTA",
    "titulo": "Meta vencida: Entregas no prazo",
    "detalhe": "Prazo terminou em 27/09/2026 com 40 de 100 (Lucas).",
    "recomendacao": "Revisar com o responsável: concluir, replanejar o prazo ou cancelar a meta.",
    "colaboradorId": 10, "departamentoId": null, "referenciaId": 1
  }]
}
```

`tipo`: `META_VENCIDA`, `META_EM_RISCO`, `INDICADOR_EM_QUEDA`, `EQUIPE_ABAIXO_DA_MEDIA` ou `SEM_ATIVIDADE_RECENTE`. `severidade`: `ALTA`, `MEDIA` ou `BAIXA`. A ordem é a das regras (metas, tendências, equipes, sem atividade); o backend ordena por severidade.

## Regras

As regras estão em [`src/regras.js`](src/regras.js) e descritas em [regras de negócio](../docs/requisitos/regras-de-negocio.md#análises-de-gestão). Elas espelham `AnaliseGestaoRegras.java`, que o backend usa como fallback. Ao mudar uma regra, altere os dois lados e rode os testes de ambos.

## Teste rápido

```bash
curl -s localhost:4000/health
curl -s -X POST localhost:4000/avaliar -H 'Content-Type: application/json' \
     -d '{"dataReferencia":"2026-09-28","colaboradoresSemAtividade":[{"colaboradorId":7,"nome":"Marina","departamentoId":2}]}'
```
