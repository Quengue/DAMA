# ADR-0008: Front-end SPA em JavaScript puro com BFF Node/Express

- **Status:** Aceita
- **Data:** 2026-09-28

## Contexto

O produto precisava de uma versão executável com front-end integrado à API. O front anterior (`projetos-aula/01-monolito-camadas/frontend`) consumia o domínio de exemplo e não a API do DAMA.

## Decisão

- SPA em JavaScript puro com módulos ES (sem etapa de build), roteamento por hash e Chart.js para gráficos.
- Servida por um BFF Node/Express que também encaminha `/api/*` para a API, preservando o caminho e o cabeçalho `X-Colaborador-Id`.
- O menu e os botões de escrita aparecem conforme as permissões devolvidas por `GET /api/me`; a autorização de fato continua no backend.

## Alternativas consideradas

- React/Angular — exigiria build, dependências e tempo de aprendizado que o prazo não comportava.
- Servir o front pelo próprio Spring Boot — perderia a camada Node pedida pela disciplina de Programação Backend.

## Consequências

- Roda com `node server.js`, sem compilação; o Dockerfile é simples.
- Sem componentes reutilizáveis de framework: o front mantém um pequeno kit próprio em `public/js/ui.js`.
- O navegador nunca acessa a API diretamente, o que evita CORS.
