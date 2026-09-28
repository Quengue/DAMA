# Deliverable 1

Entregas por disciplina.

| Pasta | Disciplina | Conteúdo |
|---|---|---|
| [AnalisePreditiva](AnalisePreditiva) | Análise Preditiva | `DAMAAnalisePreditiva1.ipynb` — pipeline de pré-processamento e modelo para `teve_incidente` |
| [EstatisticaMulti](EstatisticaMulti) | Estatística Multivariada | `relatorioinicialcompleto.rmd` — relatório inicial (lê os CSVs de `../CSVsNecessarios`) |
| [machine-learning-ia](machine-learning-ia) | Machine Learning | `main.ipynb` e a base sintética em `Data/` (CSVs, SQLite, dicionário de dados e gerador) |
| [PesquisaOperacional](PesquisaOperacional) | Pesquisa Operacional e Cálculo Aplicado | `DELIVERABLE 1 LUCY.docx` |
| [CSVsNecessarios](CSVsNecessarios) | apoio | As quatro bases usadas por Análise Preditiva e Estatística (cópia de `machine-learning-ia/Data/csv`) |

As entregas de Modelagem e Construção de Software, Programação Backend, Qualidade de Software e Projeto Integrador estão no código (`backend/`, `frontend/`, `database/`) e em [`docs/`](../docs).

## Como rodar

- **Análise Preditiva**: no Colab, envie os quatro arquivos de `CSVsNecessarios/` para a raiz do ambiente e execute o notebook.
- **Estatística Multivariada**: abra o `.rmd` no RStudio e use *Knit*; os CSVs são lidos de `../CSVsNecessarios`.
- **Machine Learning**: execute `machine-learning-ia/main.ipynb` a partir da própria pasta `machine-learning-ia/`.
