# Dicionário de Dados — Base Sintética de Utilização de IA em Empresas

Todas as tabelas abaixo são **100% sintéticas**, geradas por `generate_synthetic_ai_data.py`
com seed fixa (`SEED = 42`). Nenhum dado representa pessoas, empresas ou eventos reais.

Convenções gerais:
- Datas no formato `AAAA-MM-DD`; timestamps no formato `AAAA-MM-DD HH:MM:SS`.
- Campos em branco/nulos no CSV representam informação não disponível ou não aplicável — isso é intencional e reflete situações comuns em dados corporativos reais.
- Nenhuma tabela contém indicadores calculados (maturidade, ROI, eficiência, score, adoção %, etc.). Esses cálculos ficam a cargo do time de análise.

---

## 1. companies.csv
Cadastro de empresas, equivalente ao que existiria em um ERP/CRM corporativo.

| Campo | Tipo | Descrição |
|---|---|---|
| company_id | texto | Identificador único da empresa (ex: `C01`) |
| company_name | texto | Nome fictício da empresa |
| industry | texto | Setor de atuação (Tecnologia / Desenvolvimento de Software, Financeiro, Indústria, Varejo, Serviços, Marketing) |
| company_size | texto | Porte declarado (`Pequena`, `Média`, `Grande`) |
| employee_count | inteiro | Headcount total da empresa (inclui colaboradores sem acesso a sistemas de IA — nem todo headcount aparece em `employees.csv`) |

## 2. departments.csv
Estrutura organizacional (RH).

| Campo | Tipo | Descrição |
|---|---|---|
| department_id | texto | Identificador único do departamento |
| company_id | texto | FK → companies.company_id |
| department_name | texto | Nome do departamento |

## 3. employees.csv
Extrato de sistema de RH (HRIS), limitado a colaboradores com potencial acesso a sistemas corporativos/ferramentas de IA (não é o headcount total da empresa).

| Campo | Tipo | Descrição |
|---|---|---|
| employee_id | texto | Identificador único do colaborador (sem nomes reais) |
| company_id | texto | FK → companies.company_id |
| department_id | texto | FK → departments.department_id |
| job_role | texto | Cargo |
| seniority | texto | Nível de senioridade (`Junior`, `Pleno`, `Senior`, `Especialista`, `Gerente`) |
| hire_date | data | Data de contratação |
| cost_center | texto | Centro de custo (~2% nulo — inconsistência de cadastro) |
| active | booleano | Se o colaborador está ativo na base atual |

## 4. ai_tools.csv
Catálogo de ferramentas de IA, equivalente a um inventário de software (CMDB/SaaS Management).

| Campo | Tipo | Descrição |
|---|---|---|
| ai_tool_id | texto | Identificador único da ferramenta |
| tool_name | texto | Nome comercial da ferramenta |
| provider | texto | Fornecedor/fabricante |
| tool_category | texto | Categoria (Assistente Generativo, Copiloto de Desenvolvimento, IA Integrada a Produtividade, Plataforma Corporativa de IA, API de Modelo) |
| licensing_type | texto | Modelo de licenciamento (`Por usuário (assinatura)` ou `Consumo (pay-as-you-go)`) |

Observação: `T15 – Zendesk Answer Bot AI` é uma ferramenta que sai de uso ao longo do período simulado (permanece no catálogo, mas deixa de aparecer em logs/licenças recentes) — simula uma descontinuação real de produto.

## 5. ai_models.csv
Modelos disponíveis por ferramenta, equivalente às tabelas de preços públicas de provedores de IA.

| Campo | Tipo | Descrição |
|---|---|---|
| model_id | texto | Identificador único do modelo |
| ai_tool_id | texto | FK → ai_tools.ai_tool_id |
| model_name | texto | Nome do modelo |
| model_family | texto | Família do modelo (GPT, Claude, Gemini, Amazon Titan, PaLM, Codex, Proprietário) |
| input_price_usd_per_1k_tokens | decimal | Preço **sintético** por 1.000 tokens de entrada (USD). Nulo para ferramentas sem cobrança por token (licenciamento por assento) |
| output_price_usd_per_1k_tokens | decimal | Preço **sintético** por 1.000 tokens de saída (USD) |

**Os preços são fictícios e não devem ser usados como referência de mercado.**

## 6. ai_usage_logs.csv
Principal tabela de eventos de uso. Simula logs de um gateway/proxy corporativo de IA (ex.: proxy de API, plataforma de IA empresarial). Não contém conteúdo de prompts nem dados pessoais — apenas metadados técnicos.

| Campo | Tipo | Descrição |
|---|---|---|
| usage_id | texto | Identificador único do evento |
| timestamp | datetime | Data/hora do evento |
| employee_id | texto | FK → employees.employee_id |
| company_id | texto | FK → companies.company_id |
| department_id | texto | FK → departments.department_id |
| ai_tool_id | texto | FK → ai_tools.ai_tool_id |
| model_id | texto | FK → ai_models.model_id |
| application | texto | Contexto/aplicação de origem (ex: "Extensão VSCode", "Chat Web", "Add-in Outlook") |
| request_count | inteiro | Número de requisições agregadas no evento |
| input_tokens | inteiro | Tokens de entrada consumidos |
| output_tokens | inteiro | Tokens de saída gerados |
| latency_ms | inteiro | Latência média em milissegundos (nulo em ~2% dos casos — telemetria ausente, mais comum em falhas) |
| request_status | texto | `success`, `error`, `timeout` ou `rate_limited` |
| project_id | texto | FK → projects.project_id, preenchido apenas para uso de copilotos de desenvolvimento em empresas de software (~23% dos registros) |
| cost_center | texto | Centro de custo do colaborador no momento do evento (pode ser nulo) |

## 7. ai_billing.csv
Simula extrato de fatura/relatório FinOps mensal.

| Campo | Tipo | Descrição |
|---|---|---|
| billing_id | texto | Identificador único do lançamento |
| company_id | texto | FK → companies.company_id |
| billing_month | texto | Mês de referência (`AAAA-MM`) |
| provider | texto | Fornecedor cobrado |
| ai_tool_id | texto | FK → ai_tools.ai_tool_id |
| model_id | texto | FK → ai_models.model_id (nulo para cobrança por licença/assento, onde não se aplica) |
| cost_center | texto | Centro de custo (pode ser nulo) |
| usage_quantity | número | Quantidade cobrada (tokens totais para ferramentas de consumo; nº de licenças ativas para ferramentas por assento) |
| total_cost | decimal | Custo total do lançamento |
| currency | texto | `USD` (ferramentas de consumo, faturadas por provedores internacionais) ou `BRL` (ferramentas por assento, contratadas localmente) |

Nota: a mistura de moedas é proposital — reflete a realidade de empresas que pagam parte das ferramentas de IA em fatura internacional (USD) e parte via contrato/revenda local (BRL). Algumas combinações empresa/mês/ferramenta não têm lançamento (~3-4%), simulando atraso ou falha de integração de fatura — isso não foi corrigido de propósito.

## 8. ai_budgets.csv
Planejamento orçamentário por centro de custo, independente do gasto realizado.

| Campo | Tipo | Descrição |
|---|---|---|
| company_id | texto | FK → companies.company_id |
| cost_center | texto | Centro de custo orçado |
| year | inteiro | Ano de referência |
| month | inteiro | Mês de referência (1–12) |
| budget_amount | decimal | Valor orçado |

Esta tabela **não é comparada** com `ai_billing.csv` na geração — a análise de orçado vs. realizado (e eventual estouro) fica a cargo do time de projeto.

## 9. ai_licenses.csv
Simula extrato de um painel de administração de licenças SaaS (ex.: admin console de Copilot/ChatGPT Enterprise).

| Campo | Tipo | Descrição |
|---|---|---|
| company_id | texto | FK → companies.company_id |
| employee_id | texto | FK → employees.employee_id |
| ai_tool_id | texto | FK → ai_tools.ai_tool_id |
| license_type | texto | `Standard`, `Professional` ou `Enterprise` |
| assigned_date | data | Data de atribuição da licença |
| expiration_date | data | Data de expiração (nula para acordos corporativos "perpétuos"/enterprise) |
| license_status | texto | `Ativa`, `Expirada` ou `Revogada` |

Permite cruzar, posteriormente, quem tem licença atribuída vs. quem efetivamente aparece em `ai_usage_logs.csv` (não calculado aqui).

## 10. training_records.csv
Simula extrato de um LMS corporativo.

| Campo | Tipo | Descrição |
|---|---|---|
| training_id | texto | Identificador único do registro |
| employee_id | texto | FK → employees.employee_id |
| company_id | texto | FK → companies.company_id |
| course_name | texto | Nome do curso |
| course_category | texto | Categoria do curso |
| enrollment_date | data | Data de inscrição |
| completion_date | data | Data de conclusão (nula se não concluído) |
| completion_status | texto | `Concluido`, `Em Andamento`, `Nao Iniciado` ou `Abandonado` |
| score | inteiro | Nota final (0–100), preenchida apenas quando `Concluido` |
| duration_hours | decimal | Carga horária do curso |

## 11. approved_ai_tools.csv
Simula registro de comitê de governança de TI/Segurança da Informação.

| Campo | Tipo | Descrição |
|---|---|---|
| company_id | texto | FK → companies.company_id |
| ai_tool_id | texto | FK → ai_tools.ai_tool_id |
| approval_status | texto | `Aprovado`, `Bloqueado` ou `Em Avaliacao` |
| approval_date | data | Data da decisão |
| allowed_departments | texto | Lista de departamentos autorizados (ou "Todos os departamentos"); nula quando a ferramenta não está aprovada. Formatação de lista (`;` ou `,`) varia entre registros — inconsistência proposital de cadastro manual |

## 12. security_events.csv
Simula log de uma ferramenta de segurança tipo CASB/DLP.

| Campo | Tipo | Descrição |
|---|---|---|
| event_id | texto | Identificador único do evento |
| company_id | texto | FK → companies.company_id |
| employee_id | texto | FK → employees.employee_id |
| timestamp | datetime | Data/hora do evento |
| ai_tool_id | texto | FK → ai_tools.ai_tool_id |
| event_type | texto | Tipo do evento (bloqueio, alerta de DLP, uso não aprovado/Shadow AI, etc.) |
| severity | texto | `Baixa`, `Media`, `Alta` ou `Critica` |
| policy_id | texto | Identificador da política violada |
| action_taken | texto | Ação tomada pelo sistema/equipe de segurança |

## 13. projects.csv
Cadastro de projetos (apenas empresas de desenvolvimento de software), equivalente a um Jira/Azure DevOps.

| Campo | Tipo | Descrição |
|---|---|---|
| project_id | texto | Identificador único do projeto |
| company_id | texto | FK → companies.company_id |
| project_name | texto | Nome do projeto |
| project_type | texto | `Produto Interno`, `Cliente`, `Plataforma`, `Mobile` ou `API/Integracao` |
| start_date | data | Data de início |
| status | texto | `Ativo`, `Concluido` ou `Pausado` |

## 14. pull_requests.csv
Simula extrato da API do GitHub/GitLab.

| Campo | Tipo | Descrição |
|---|---|---|
| pull_request_id | texto | Identificador único do PR |
| project_id | texto | FK → projects.project_id |
| employee_id | texto | FK → employees.employee_id (autor) |
| created_at | datetime | Data/hora de criação |
| merged_at | datetime | Data/hora do merge (nulo se não mesclado) |
| status | texto | `Merged`, `Fechado sem merge` ou `Aberto` |
| files_changed | inteiro | Nº de arquivos alterados |
| additions | inteiro | Linhas adicionadas |
| deletions | inteiro | Linhas removidas |
| review_count | inteiro | Nº de revisões (nulo em ~5% dos casos — dado não capturado) |

Nenhum campo indica se o código foi "gerado por IA" — essa correlação deve ser explorada futuramente cruzando com `ai_usage_logs.csv`.

## 15. issues.csv
Simula extrato de sistema de tickets (Jira).

| Campo | Tipo | Descrição |
|---|---|---|
| issue_id | texto | Identificador único do issue |
| project_id | texto | FK → projects.project_id |
| employee_id | texto | FK → employees.employee_id (responsável) |
| issue_type | texto | `Bug`, `Feature`, `Debito Tecnico` ou `Chore` |
| priority | texto | `Baixa`, `Media`, `Alta` ou `Critica` |
| created_at | datetime | Data/hora de criação |
| closed_at | datetime | Data/hora de fechamento (nulo se aberto) |
| status | texto | `Aberto`, `Em Andamento` ou `Fechado` |

## 16. builds.csv
Simula log de pipeline de CI (Jenkins/GitHub Actions).

| Campo | Tipo | Descrição |
|---|---|---|
| build_id | texto | Identificador único do build |
| project_id | texto | FK → projects.project_id |
| timestamp | datetime | Data/hora da execução |
| status | texto | `Sucesso` ou `Falha` |
| duration | inteiro | Duração em segundos |
| tests_executed | inteiro | Nº de testes executados |
| tests_failed | inteiro | Nº de testes que falharam |

## 17. deployments.csv
Simula log de pipeline de CD.

| Campo | Tipo | Descrição |
|---|---|---|
| deployment_id | texto | Identificador único do deploy |
| project_id | texto | FK → projects.project_id |
| timestamp | datetime | Data/hora do deploy |
| environment | texto | `Dev`, `Staging` ou `Producao` |
| status | texto | `Sucesso`, `Falha` ou `Revertido` |

---

## Relacionamentos (chaves estrangeiras)

- `companies.company_id` ← departments, employees, ai_billing, ai_budgets, ai_licenses, training_records, approved_ai_tools, security_events, projects, ai_usage_logs
- `departments.department_id` ← employees, ai_usage_logs
- `employees.employee_id` ← ai_usage_logs, ai_licenses, training_records, security_events, pull_requests, issues
- `ai_tools.ai_tool_id` ← ai_models, ai_usage_logs, ai_billing, ai_licenses, approved_ai_tools, security_events
- `ai_models.model_id` ← ai_usage_logs, ai_billing
- `projects.project_id` ← pull_requests, issues, builds, deployments; também referenciado por `ai_usage_logs.project_id` quando aplicável
- `cost_center` é um atributo comum (não uma FK rígida) presente em employees, ai_usage_logs, ai_billing e ai_budgets, útil para cruzamentos financeiros/organizacionais

## Como as diferenças de comportamento emergem (sem colunas de indicador)

Nenhuma tabela informa diretamente se uma empresa é "madura" ou "avançada" em IA. As diferenças de intensidade de uso, velocidade de adoção, cobertura de licenciamento e crescimento de custo são resultado de parâmetros usados **apenas durante a geração** (nunca exportados), como intensidade-base por empresa, viés de uso por departamento, mês de início de adoção e tendência de crescimento. Essas diferenças só se tornam visíveis analisando os registros brutos — exatamente como aconteceria com dados reais.
