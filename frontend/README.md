# Front-end

SPA em JavaScript puro (módulos ES, sem build) servida por um BFF Node/Express. O BFF entrega os arquivos de `public/` e encaminha `/api/*` para a API, repassando o cabeçalho `X-Colaborador-Id`.

```
frontend/
├── server.js              BFF: estáticos, proxy /api, /health e Chart.js em /vendor
└── public/
    ├── index.html
    ├── css/app.css
    └── js/
        ├── app.js         rotas, menu por permissão e login
        ├── api.js         cliente HTTP
        ├── sessao.js      usuário logado e permissões
        ├── dados.js       listas de apoio com cache (pessoas, equipes, indicadores)
        ├── ui.js          componentes (tabela, painel, formulário, modal, toast)
        ├── graficos.js    Chart.js
        └── paginas/       uma página por item do menu
```

## Rodar

```bash
npm install
API_BASE_URL=http://localhost:8080 npm start     # http://localhost:3000
```

`PORT` muda a porta do front. No PowerShell: `$env:API_BASE_URL="http://localhost:8080"; npm start`.

## Telas

| Menu | Quem vê | Conteúdo |
|---|---|---|
| Meu painel | todos | Pontos, AI Credits, nível, conquistas, metas e evolução dos indicadores |
| Evolução e conquistas | todos | Níveis, progresso e conquistas; gestores criam e concedem conquistas |
| AI Credits | todos | Saldo, nível de autonomia, histórico; gestores registram atividades e lançam créditos |
| Ranking e pontuação | todos | Ranking da empresa; gestores lançam pontos e mantêm regras |
| Metas | todos | Metas com progresso; gestores criam metas e registram progresso |
| Indicadores | todos | Evolução por pessoa ou equipe com filtro de período |
| Equipes | todos | Dashboard da equipe; gestores comparam equipes |
| Desafios / Reconhecimentos | todos | Listagem; gestores criam, inscrevem e concluem |
| Análises de gestão | gestores | Alertas de onde agir |
| Pessoas e perfis | gestores | Cadastro de colaboradores; administrador gerencia perfis e equipes |
| Auditoria | administrador | Movimentações registradas |
