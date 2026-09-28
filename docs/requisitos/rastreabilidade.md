# Rastreabilidade: história → requisito → implementação → teste

Histórias do Jira-PO (épico POAN-109). A numeração de RFs segue a das histórias no Jira.

| História | Requisitos | Módulo / endpoints | Tela | Testes |
|---|---|---|---|---|
| POAN-110 Cadastro e gestão de colaboradores | RF01, RF02, RF16, RF17 | `colaborador`, `organizacao` — `/empresas/{id}/colaboradores`, `/colaboradores/{id}` | Pessoas e perfis | `DamaApiIntegrationTest` (cadastro em todos os cenários) |
| POAN-111 Perfis e permissões | RF18, RN01, RN07 | `perfil`, `shared.security`, `sessao` — `/perfis`, `/me` | Pessoas e perfis, login | `perfilColaboradorVeRankingMasNaoVeAlertasNemAuditoria`, `requisicaoSemCabecalhoNaoEhAutenticada` |
| POAN-112 Indicadores, evolução e filtro por período | RF03, RF06, RF34, RF35 | `indicador` — `/indicadores/{id}/valores?inicio&fim` | Indicadores | `alertasApontamMetaVencidaEIndicadorEmQueda` (registro de valores) |
| POAN-113 Dashboard do colaborador | RF05, RF32 | `dashboard` — `/colaboradores/{id}/dashboard` | Meu painel | verificado no front e na demo |
| POAN-114 Metas e progresso | RF04, RF29, RF30, RN05 | `meta` — `/metas/{id}/progresso` | Metas | `MetaServiceTest`, `metaDeIndicadorMenorEhMelhorConcluiAoFicarAbaixoDoAlvo` |
| POAN-115 Pontuação, histórico e ranking | RF07, RF08, RF11, RF27, RF28, RF37 | `pontuacao`, `analytics` — `/colaboradores/{id}/pontuacao`, `/analytics/ranking-colaboradores` | Ranking e pontuação | `visaoConsolidadaSomaPontosSemMultiplicarPorDepartamentosEMetas` |
| POAN-116 Desafios e reconhecimentos | RF09, RF10 | `desafio`, `reconhecimento` | Desafios, Reconhecimentos | `conquistaCriadaDepoisVaiParaQuemJaCumpreOCriterio` (reconhecimento) |
| POAN-117 AI Credits | RF12, RF19–RF26 | `aicredit` — atividades, regras, saldo, histórico, níveis | AI Credits | verificado no front e na demo |
| POAN-118 Visão consolidada e comparação entre equipes | RF13, RF14, RF33, RF36 | `analytics` — `/visao-consolidada`, `/ranking-departamentos`, `/indicadores-departamentos` | Análises, Equipes | `visaoConsolidadaSomaPontosSemMultiplicarPorDepartamentosEMetas` |
| POAN-119 Auditoria | RF38 | `auditoria` — `/empresas/{id}/auditoria` | Auditoria | `perfilColaborador...` (acesso negado ao Colaborador) |
| POAN-120 Conquistas e níveis de evolução | RF31 | `conquista` — `/conquistas`, `/evolucao`, `/niveis-evolucao` | Evolução e conquistas | `ConquistaServiceTest`, `conquistaAutomaticaEhConcedidaAoAtingirOCriterioENivelSobe`, `conquistaManualEhConcedidaUmaVezSo` |
| POAN-121 Análises de gestão e dashboard por equipe | RF15, RF32/RF33 (equipe) | `analytics` — `/analytics/alertas`; `dashboard` — `/departamentos/{id}/dashboard` | Análises de gestão, Equipes | `AnaliseGestaoRegrasTest`, `alertasApontamMetaVencidaEIndicadorEmQueda`, `dashboardDaEquipeConsolidaMembros` |
| POAN-122 Autenticação real e isolamento entre empresas | RNF02, RN01, RN07 | não implementado | — | — |

## Critérios de aceitação do documento (seção 8.5)

| Critério | Onde é atendido |
|---|---|
| US07 — atribuir AI Credits: escolher colaborador e atividade, registrar movimentação, somar ao saldo, aparecer no histórico | AI Credits → "Registrar atividade" e "Crédito ou débito manual"; `POST /colaboradores/{id}/atividades` e `/ai-credits/transacoes` |
| US10 — pontuação calculada automaticamente e atualizada após cada movimentação | Pontos sempre somados da tabela `pontuacao` (nunca armazenados); ranking lê `vw_ranking_colaborador` |
| US12 — criar meta para colaborador ou equipe, com objetivo, valor esperado e período | Metas → "Nova meta"; RN05 validado no backend |
| US15 — dashboard da equipe com dados registrados, evolução e respeito às permissões | Equipes; `GET /departamentos/{id}/dashboard` com `DESEMPENHO_LER` |

## Como rodar os testes

```bash
cd backend
mvn test
```

`DamaApiIntegrationTest` sobe um PostgreSQL 16 com Testcontainers e aplica os scripts de `database/`; sem Docker disponível ele é pulado e os testes unitários continuam rodando.
