# Produto

- [DocVisao.doc](DocVisao.doc) — documento de visão do produto (Deliverable 1).
- [DAMA-Intelligence-Documento-Completo.docx](DAMA-Intelligence-Documento-Completo.docx) — ideia central, hipótese de inovação, personas, regras de negócio, requisitos, MVPs, backlog, critérios de aceitação e a análise de negócio e marketing.

## Resumo

O DAMA Intelligence transforma o uso produtivo de IA em um sistema mensurável de desempenho, evolução e autonomia. A plataforma acompanha indicadores e metas, pontua e reconhece colaboradores e concede **AI Credits** por atividades produtivas; o saldo de créditos define o nível de autonomia de acesso às ferramentas de IA da empresa.

Personas usadas na demonstração (`database/demo/04_demo.sql`):

| Persona | Na demo | O que usa |
|---|---|---|
| Gestora de pessoas | Mariana Souza (#2, perfil Gestor) | Dashboard da equipe, metas, análises de gestão |
| Colaborador | Lucas Pereira (#4, perfil Colaborador) | Meu painel, evolução, conquistas, ranking, AI Credits |
| Diretor | Ricardo Nogueira (#3, perfil Gestor) | Visão consolidada e comparação entre equipes |

## O que está implementado

| Área | Situação |
|---|---|
| Colaboradores, equipes, perfis e permissões | Implementado |
| Indicadores, evolução e filtro por período | Implementado |
| Metas e progresso (inclui indicadores "menor é melhor") | Implementado |
| Pontuação, ranking, desafios e reconhecimentos | Implementado |
| AI Credits: atividades, elegibilidade, saldo, histórico e níveis de autonomia | Implementado |
| Conquistas e níveis de evolução | Implementado |
| Dashboard individual e por equipe, visão consolidada, comparação entre equipes | Implementado |
| Análises de gestão (metas vencidas/em risco, quedas, equipes abaixo da média, engajamento) | Implementado |
| Auditoria de movimentações | Implementado |
| Login real (JWT/SSO) e isolamento entre empresas | Não implementado — ver [ADR-0006](../adr/0006-autenticacao-por-cabecalho.md) |
| AI Literacy Score (eficiência no uso de tokens) | Não implementado |
| Relatórios exportáveis e previsão de desempenho (ML) | Não implementado |
