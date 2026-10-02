# Projeto 1 — Monólito em Camadas + Serviço Node.js

Mesmo domínio da trilha: projetos de IA e registro de consumo de tokens. Organização horizontal: `controller -> service -> repository -> entity`.

## Evolução desta versão
Foi adicionado um serviço Node.js independente (`alert-service`) responsável por avaliar o consumo de tokens e produzir alertas didáticos.

Fluxo ao registrar consumo:

`Front Node -> Spring Boot /api/usages -> UsageService -> PostgreSQL -> AlertClient -> REST -> Node alert-service`

O front-end **continua chamando apenas o monólito**. O Spring Boot atua como orquestrador da operação e devolve na mesma resposta os dados de consumo e o alerta produzido pelo Node.js.

### Por que isso é didaticamente importante?
- `UsageService` depende da interface `AlertClient`, não de HTTP nem de Node.js diretamente.
- `NodeAlertClient` é o adaptador de infraestrutura que conhece REST e a URL do serviço Node.
- Trocar Node.js por outro mecanismo exigiria principalmente outra implementação de `AlertClient`.
- Apesar desse desacoplamento de código, agora existe **dependência de rede**: a operação de registro chama um processo externo.

## Serviços
- Front-end Node/Express: http://localhost:3000
- Backend Spring Boot: http://localhost:8080
- Alert Service Node.js: http://localhost:3001
- PostgreSQL: localhost:5432

## Executar

```bash
docker compose up --build
```

<<<<<<< Updated upstream
Teste de saúde do Node:
=======
Variáveis: `DB_HOST` (localhost), `DB_PORT` (5432), `DB_NAME`, `DB_USER`, `DB_PASSWORD` (dama). O banco precisa dos scripts de [`../database`](../database).

Alertas de gestão: `ALERTAS_URL` (vazia por padrão) aponta para o [`alert-service`](../alert-service), por exemplo `http://localhost:4000`; `ALERTAS_TIMEOUT_MS` (2000) limita a espera. Sem a URL, ou se o serviço falhar, a API aplica as regras localmente ([ADR-0009](../docs/adr/0009-alert-service-node-para-alertas-de-gestao.md)).

## Testes
>>>>>>> Stashed changes

```bash
curl http://localhost:3001/health
```

<<<<<<< Updated upstream
## Limiares didáticos do alert-service
- abaixo de 5.000 tokens: INFO
- de 5.000 a 9.999: WARNING
- a partir de 10.000: CRITICAL
=======
- `analytics/internal/AnaliseGestaoRegrasTest`, `conquista/internal/ConquistaServiceTest`, `meta/internal/MetaServiceTest`: regras de negócio, sem banco.
- `analytics/internal/AlertaGestaoNodeTest`: contrato com o alert-service e fallback para as regras locais, com um servidor HTTP simulado (sem Docker).
- `DamaApiIntegrationTest`: API inteira contra PostgreSQL 16 (Testcontainers) com os scripts de `database/`. Pulado se não houver Docker.
>>>>>>> Stashed changes

## Deployment
1. Maven produz um JAR do monólito.
2. Docker produz uma imagem do backend Spring Boot.
3. Docker produz uma imagem independente do serviço Node.js.
4. Compose sobe frontend + backend + alert-service + PostgreSQL.
5. O Node pode ser atualizado separadamente, mas o registro de consumo atualmente depende de sua disponibilidade por ser uma chamada REST síncrona.

## Correção do proxy do front
O front usa URLs `/api/projects` e `/api/usages`. Como o Express monta o proxy em `/api`, o prefixo é removido antes de o middleware receber a rota. O `frontend/server.js` desta versão recoloca explicitamente `/api` via `pathRewrite`, garantindo:

`Front /api/projects -> Spring Boot /api/projects`

`Front /api/usages -> Spring Boot /api/usages`

Se aparecer `404` com `path: "/projects"`, a imagem antiga do front ainda está em uso. Reconstrua tudo com:

```bash
docker compose down
docker compose up --build --force-recreate
```

## Correção BFF v2
O frontend preserva explicitamente o caminho `/api/*` ao encaminhar para o Spring Boot.
Exemplo: `/api/projects` -> `http://backend:8080/api/projects`.

Para evitar container/imagem antigos:
```powershell
docker compose down --volumes --remove-orphans
docker compose build --no-cache frontend backend alert-service
docker compose up --force-recreate
```


## Versão 3 - correção do backend Spring Boot

Nesta versão, o `NodeAlertClient` não depende mais de injeção de `RestClient.Builder`.
O próprio adaptador cria o `RestClient` com `RestClient.builder()`, evitando a falha de inicialização do contexto do Spring Boot observada na versão anterior.

### Execução recomendada

```powershell
docker compose down --volumes --remove-orphans
docker compose build --no-cache
docker compose up --force-recreate
```

Depois valide:

```powershell
docker compose ps
```

Os serviços `db`, `backend`, `frontend` e `alert-service` devem estar `Up`.
