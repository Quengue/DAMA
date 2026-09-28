# Projetos-modelo das aulas

Evolução arquitetural estudada nas aulas com o domínio de exemplo (projetos de IA e consumo de tokens). Não fazem parte do produto; o DAMA Intelligence está em `backend/` e `frontend/` na raiz.

| Pasta | Arquitetura | ADR |
|---|---|---|
| [01-monolito-camadas](01-monolito-camadas) | Monólito em camadas + serviço Node.js de alertas + front Node | [0001](../docs/adr/0001-monolito-em-camadas.md), [0004](../docs/adr/0004-alert-service-node-via-alertclient.md) |
| [02-monolito-modular](02-monolito-modular) | Monólito modular (`project`, `usage`) | [0002](../docs/adr/0002-monolito-modular.md) |
| [03-microservices](03-microservices) | `project-service` + `usage-service` + `alert-service` | [0003](../docs/adr/0003-microservices-como-estudo.md) |

Cada pasta sobe sozinha com `docker compose up --build` dentro dela. Não rode junto com a stack do DAMA: as portas 3000, 8080 e 5432 são as mesmas.
