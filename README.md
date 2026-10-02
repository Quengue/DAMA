# DAMA Intelligence
<<<<<<< Updated upstream
=======

Plataforma que transforma o uso produtivo de IA em desempenho mensurável: indicadores, metas, pontuação, conquistas e **AI Credits**, que liberam níveis maiores de autonomia no acesso às ferramentas de IA da empresa.

Projeto do 4º semestre de Ciência de Dados e Negócios da ESPM (2026) — Ana Luiza Proença, Gustavo Gomes, Kaique Lira e Ricardo Matteo.

## Estrutura do repositório

```
DAMA/
├── backend/          API REST — Spring Boot 3, Java 17, monólito modular
├── frontend/         Front-end (SPA em JavaScript) + BFF Node/Express
├── alert-service/    Serviço Node.js dos alertas de gestão (chamado pelo backend)
├── database/         Scripts PostgreSQL: schema, views, seed e dados de demonstração
├── docs/             Produto, arquitetura (C4), ADRs, API e rastreabilidade
├── Deliverable1/     Entregas acadêmicas do Deliverable 1 por disciplina
├── projetos-aula/    Projetos-modelo das aulas (camadas, modular, microservices)
└── docker-compose.yml
```

## Como rodar

Pré-requisito: Docker.

```bash
docker compose up --build
```

Abra http://localhost:3000 e entre com um dos usuários de demonstração:

| ID | Usuário | Perfil |
|---|---|---|
| 1 | Administrador | Administrador (acesso total) |
| 2 | Mariana Souza | Gestor |
| 3 | Ricardo Nogueira | Gestor |
| 4 | Lucas Pereira | Colaborador |

| Serviço | Endereço |
|---|---|
| Front-end | http://localhost:3000 |
| API | http://localhost:8080/api (saúde em `/actuator/health`) |
| Serviço de alertas | http://localhost:4000 (saúde em `/health`) |
| PostgreSQL | localhost:5432, banco/usuário/senha `dama` |

Para recriar o banco do zero (necessário depois de mudar os scripts de `database/`):

```bash
docker compose down -v
```

Portas e credenciais podem ser alteradas copiando `.env.example` para `.env`. Para subir sem os dados de demonstração, remova a linha do `04_demo.sql` no `docker-compose.yml`.

## Desenvolvimento sem Docker

```bash
# 1. PostgreSQL local com os scripts de database/ aplicados (01, 02, 03 e, se quiser, demo/04)
# 2. Serviço de alertas (opcional: sem ALERTAS_URL a API usa as regras locais)
cd alert-service && npm start
# 3. API
cd backend && ALERTAS_URL=http://localhost:4000 mvn spring-boot:run
# 4. Front-end (em outro terminal)
cd frontend && npm install && API_BASE_URL=http://localhost:8080 npm start
```

## Testes

```bash
cd backend && mvn test
cd alert-service && npm test
```

Unitários para regras de metas, conquistas e análises de gestão; testes de integração da API contra PostgreSQL real via Testcontainers (exigem Docker, senão são pulados). No `alert-service`, as regras e a API HTTP são testadas com `node --test` (sem dependências). `AlertaGestaoNodeTest` cobre o contrato e o fallback do backend com um servidor HTTP simulado.

## Documentação

- [Produto e requisitos](docs/produto/)
- [Arquitetura C4](docs/arquitetura/) e [ADRs](docs/adr/)
- [Referência da API](docs/api/)
- [Rastreabilidade e regras de negócio](docs/requisitos/)
>>>>>>> Stashed changes
