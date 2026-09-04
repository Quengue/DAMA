# Microservices + Node.js REST síncrono

Evolução do projeto original com um `alert-service` independente em Node.js.

## Serviços
- `project-service` — Java/Spring Boot — porta 8081 — PostgreSQL `projects-db`
- `usage-service` — Java/Spring Boot — porta 8082 — PostgreSQL `usages-db`
- `alert-service` — Node.js/Express — porta 3001 — PostgreSQL `alerts-db`
- `frontend` — Node.js/Express BFF — porta 3000

## Comunicação síncrona
Ao registrar um consumo, `usage-service` valida o projeto em `project-service`, persiste o consumo e chama `alert-service` por REST. A resposta do consumo só é concluída após o Node.js devolver `INFO`, `WARNING` ou `CRITICAL`.

## Execução
```bash
docker compose down --volumes --remove-orphans
docker compose build --no-cache
docker compose up --force-recreate
```

Abra: `http://localhost:3000`

## Teste pela tela
1. Crie `Projeto Teste Node`, área `IA`.
2. Clique em `Atualizar` e copie o UUID.
3. Cole o UUID em `Registrar consumo`.
4. Use `6500` tokens e `gpt-demo`.
5. Clique em `Registrar` — esperado: `WARNING`.
6. Clique em `Atualizar alertas` para comprovar que o alerta foi persistido pelo serviço Node.js.

Valores didáticos:
- 100 tokens → INFO
- 6500 tokens → WARNING
- 12000 tokens → CRITICAL
