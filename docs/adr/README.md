# ADRs — Registros de decisão de arquitetura

Cada decisão relevante vira um arquivo numerado a partir do [template](0000-template.md). Um ADR aceito não é editado para mudar de ideia: cria-se um novo que o substitui.

| # | Decisão | Status |
|---|---|---|
| [0001](0001-monolito-em-camadas.md) | Primeira versão como monólito em camadas | Substituída pela 0002 |
| [0002](0002-monolito-modular.md) | Evoluir para monólito modular | Aceita |
| [0003](0003-microservices-como-estudo.md) | Microservices mantidos como estudo, não como arquitetura do produto | Aceita |
| [0004](0004-alert-service-node-via-alertclient.md) | Serviço Node.js de alertas acessado pela interface `AlertClient` | Aceita (projetos de aula) |
| [0005](0005-jdbc-e-views-sql.md) | Persistência com JdbcTemplate e views SQL, sem JPA | Aceita |
| [0006](0006-autenticacao-por-cabecalho.md) | Autenticação simplificada por cabeçalho `X-Colaborador-Id` | Aceita, provisória |
| [0007](0007-conquistas-por-eventos-de-dominio.md) | Conquistas avaliadas por evento de domínio | Aceita |
| [0008](0008-front-spa-com-bff-node.md) | Front-end SPA em JavaScript puro com BFF Node/Express | Aceita |
| [0009](0009-alert-service-node-para-alertas-de-gestao.md) | Alertas de gestão calculados por um serviço Node.js (`alert-service`) | Aceita |
