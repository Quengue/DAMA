# Base Sintética — Utilização de IA em Empresas

## O que é isto

Este pacote contém uma base de dados **integralmente sintética**, criada para simular o
tipo de dado que empresas reais poderiam ter registrado em seus sistemas (RH, TI, FinOps,
LMS, governança de segurança e ferramentas de desenvolvimento) sobre a utilização de
Inteligência Artificial no ambiente corporativo.

**Os dados são integralmente sintéticos e não representam indivíduos ou empresas reais.**
Nomes de empresas, colaboradores, valores financeiros e preços de modelos de IA são
fictícios e gerados programaticamente.

## Objetivo

Servir como fonte bruta para um projeto futuro de análise de adoção, consumo, custos e
utilização de IA em empresas. Por isso, **nenhuma tabela contém indicadores, scores ou
KPIs pré-calculados** (maturidade, ROI, eficiência, percentual de adoção, economia de
tempo, risco de estouro de orçamento, etc.). Todos os campos são dados brutos que
poderiam plausivelmente existir em sistemas corporativos reais — o cálculo de qualquer
indicador fica a cargo do time de análise que utilizar esta base.

As diferenças de comportamento entre empresas (uso intenso vs. moderado vs. baixo,
adoção tardia, crescimento de custo, excesso de licenciamento com baixo uso, etc.) não
estão rotuladas em nenhuma coluna — elas emergem naturalmente da combinação dos
registros brutos, como aconteceria em dados reais.

## Conteúdo do pacote

```
synthetic_ai_usage_dataset/
├── csv/                          17 tabelas em formato .csv
├── database/
│   └── ai_usage_synthetic.db     mesmas 17 tabelas em SQLite
├── generate_synthetic_ai_data.py script gerador (Python)
├── data_dictionary.md            dicionário de dados completo
└── README.md                     este arquivo
```

### Tabelas incluídas

Organização: `companies`, `departments`, `employees`.
Catálogo de IA: `ai_tools`, `ai_models`.
Utilização: `ai_usage_logs`.
Financeiro: `ai_billing`, `ai_budgets`.
Licenciamento: `ai_licenses`.
Capacitação: `training_records`.
Governança: `approved_ai_tools`, `security_events`.
Desenvolvimento de software (apenas empresas do setor): `projects`, `pull_requests`,
`issues`, `builds`, `deployments`.

Consulte `data_dictionary.md` para a descrição completa de cada tabela e coluna.

## Escopo simulado

- 18 empresas fictícias, cobrindo os setores Tecnologia/Desenvolvimento de Software (5),
  Financeiro (3), Indústria (3), Varejo (3), Serviços (2) e Marketing (2), de portes
  pequeno, médio e grande.
- 14 meses de histórico de utilização (junho/2025 a julho/2026).
- Mais de 2.300 colaboradores, ~270 mil eventos de uso de IA, faturas, licenças,
  treinamentos, eventos de segurança e (para as empresas de software) atividade de
  desenvolvimento (PRs, issues, builds, deployments).

## Reprodutibilidade

O script usa uma seed fixa (`SEED = 42`, definida no topo de
`generate_synthetic_ai_data.py`) tanto para `random` quanto para `numpy.random`. Rodar o
script novamente sem alterações produz exatamente a mesma base.

### Como gerar novamente

```bash
pip install pandas numpy
python3 generate_synthetic_ai_data.py
```

O script escreve por padrão em `/tmp/synthetic_ai_usage_build` (para evitar problemas de
E/S em filesystems de rede); ajuste a variável de ambiente `SYNTH_BUILD_DIR` para mudar o
destino, e copie o conteúdo gerado para onde desejar depois.

## Qualidade dos dados (propositalmente imperfeita)

Para simular imperfeições comuns em dados corporativos reais, a base inclui, em pequena
proporção:

- Campos opcionais nulos (ex.: `cost_center`, `expiration_date`, `latency_ms`,
  `merged_at`, `closed_at`, `completion_date`, `review_count`).
- Uma ferramenta de IA que aparece nos primeiros meses e depois some da base (simulando
  descontinuação de produto).
- Meses/faturas ausentes em `ai_billing.csv` (falha ou atraso de integração).
- Formatação levemente inconsistente em `approved_ai_tools.allowed_departments`.
- Departamentos e colaboradores sem nenhum uso de IA em determinados meses (ou nunca).
- Pequena variação de moeda entre tabelas de custo (USD para ferramentas de consumo via
  API, BRL para licenças contratadas localmente) — proposital, não corrigido.

Essas imperfeições foram mantidas moderadas: a base permanece consistente e utilizável
(sem violação de integridade referencial — chaves estrangeiras foram validadas).

## O que este pacote **não** inclui

- Conteúdo de prompts ou qualquer texto gerado pelas ferramentas de IA.
- Dados pessoais (nomes reais, e-mails, documentos).
- Qualquer indicador, score, classificação de maturidade ou recomendação.
- Marcação de "gerado por IA" em código, PRs, issues, builds ou deployments — essa
  correlação, se existir, deve ser explorada cruzando `ai_usage_logs` com as tabelas de
  desenvolvimento.

## Uso sugerido

Abrir os `.csv` em qualquer ferramenta de análise (Excel, Python/pandas, R) ou conectar
diretamente ao arquivo `database/ai_usage_synthetic.db` (SQLite) para consultas SQL.
