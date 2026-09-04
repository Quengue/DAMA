#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
generate_synthetic_ai_data.py

Gera uma base de dados SINTETICA representando o que empresas poderiam
plausivelmente ter registrado em seus sistemas (RH, TI, FinOps, LMS,
governanca, DevOps) sobre a utilizacao de Inteligencia Artificial.

IMPORTANTE:
- Nenhum dado real de pessoas, empresas ou precos e utilizado.
- Nao sao gerados indicadores/KPIs pre-calculados (maturidade, ROI,
  eficiencia, score, etc.). Apenas dados brutos plausiveis.
- Seed fixa para reprodutibilidade: ver SEED abaixo.

Saida:
  csv/*.csv                          -> uma tabela por arquivo
  database/ai_usage_synthetic.db     -> mesmo conteudo em SQLite
"""

import os
import random
import sqlite3
import numpy as np
import pandas as pd
from datetime import date, timedelta

# --------------------------------------------------------------------------
# CONFIGURACAO GERAL
# --------------------------------------------------------------------------

SEED = 42
random.seed(SEED)
np.random.seed(SEED)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
# Gera sempre em um diretorio de build local (nao no mount FUSE de outputs,
# que nao permite sobrescrever/apagar arquivos ja gravados). A copia para a
# pasta de entrega final e feita separadamente, uma unica vez.
OUT_DIR = os.environ.get("SYNTH_BUILD_DIR", "/tmp/synthetic_ai_usage_build")
CSV_DIR = os.path.join(OUT_DIR, "csv")
DB_DIR = os.path.join(OUT_DIR, "database")
DB_PATH = os.path.join(DB_DIR, "ai_usage_synthetic.db")

import shutil as _shutil
if os.path.exists(OUT_DIR):
    _shutil.rmtree(OUT_DIR)

os.makedirs(CSV_DIR, exist_ok=True)
os.makedirs(DB_DIR, exist_ok=True)

TODAY = date(2026, 8, 10)

# 14 meses de historico de uso (>= 12 meses exigidos)
MONTHS = list(pd.period_range("2025-06", "2026-07", freq="M"))
N_MONTHS = len(MONTHS)


def month_bounds(period):
    start = period.to_timestamp().date()
    end = period.to_timestamp(how="end").date()
    return start, end


def rand_date_in_month(period):
    start, end = month_bounds(period)
    delta = (end - start).days
    return start + timedelta(days=random.randint(0, delta))


def rand_datetime_in_month(period):
    d = rand_date_in_month(period)
    return pd.Timestamp(
        year=d.year, month=d.month, day=d.day,
        hour=random.randint(7, 21), minute=random.randint(0, 59),
        second=random.randint(0, 59),
    )


def clip(v, lo, hi):
    return max(lo, min(hi, v))


# --------------------------------------------------------------------------
# 1. EMPRESAS (simula cadastro de clientes/empresas em um ERP/CRM)
# --------------------------------------------------------------------------
# Campos internos (intensity, start_m, growth, overprov, software) sao usados
# APENAS para parametrizar a geracao dos dados brutos abaixo. Eles NAO sao
# exportados em nenhum CSV -- as diferencas de comportamento devem emergir
# dos registros (logs, licencas, custos), nunca de uma coluna de rotulo.

COMPANIES_RAW = [
    dict(id="C01", name="NexoCode Solucoes", industry="Tecnologia / Desenvolvimento de Software",
         size="Grande", emp_count=1450, software=True, intensity=0.85, start_m=0, growth=True, overprov=0.9),
    dict(id="C02", name="Vortice Software", industry="Tecnologia / Desenvolvimento de Software",
         size="Media", emp_count=420, software=True, intensity=0.80, start_m=0, growth=True, overprov=1.0),
    dict(id="C03", name="Piloto Digital Sistemas", industry="Tecnologia / Desenvolvimento de Software",
         size="Pequena", emp_count=140, software=True, intensity=0.55, start_m=2, growth=True, overprov=1.1),
    dict(id="C04", name="Cronos Tech Labs", industry="Tecnologia / Desenvolvimento de Software",
         size="Media", emp_count=380, software=True, intensity=0.75, start_m=0, growth=False, overprov=0.95),
    dict(id="C05", name="ByteFlow Engenharia de Software", industry="Tecnologia / Desenvolvimento de Software",
         size="Pequena", emp_count=95, software=True, intensity=0.25, start_m=6, growth=False, overprov=2.2),
    dict(id="C06", name="Banco Alterosa S.A.", industry="Financeiro",
         size="Grande", emp_count=3200, software=False, intensity=0.40, start_m=1, growth=True, overprov=1.3),
    dict(id="C07", name="Capital Sul Investimentos", industry="Financeiro",
         size="Media", emp_count=310, software=False, intensity=0.15, start_m=8, growth=False, overprov=2.5),
    dict(id="C08", name="Fincredi Solucoes Financeiras", industry="Financeiro",
         size="Pequena", emp_count=180, software=False, intensity=0.35, start_m=3, growth=True, overprov=1.2),
    dict(id="C09", name="Metalurgica Vale Forte", industry="Industria",
         size="Grande", emp_count=2600, software=False, intensity=0.12, start_m=5, growth=False, overprov=3.0),
    dict(id="C10", name="Quimica Bandeirantes", industry="Industria",
         size="Media", emp_count=650, software=False, intensity=0.18, start_m=4, growth=True, overprov=1.8),
    dict(id="C11", name="TextilNorte Manufatura", industry="Industria",
         size="Pequena", emp_count=210, software=False, intensity=0.05, start_m=10, growth=False, overprov=4.0),
    dict(id="C12", name="Compra Certa Varejo", industry="Varejo",
         size="Grande", emp_count=1900, software=False, intensity=0.30, start_m=2, growth=True, overprov=1.4),
    dict(id="C13", name="Lojas Formosa", industry="Varejo",
         size="Media", emp_count=540, software=False, intensity=0.10, start_m=7, growth=False, overprov=2.8),
    dict(id="C14", name="MercadoPrime E-commerce", industry="Varejo",
         size="Media", emp_count=460, software=False, intensity=0.65, start_m=0, growth=True, overprov=0.85),
    dict(id="C15", name="Prisma Consultoria Empresarial", industry="Servicos",
         size="Media", emp_count=290, software=False, intensity=0.70, start_m=0, growth=True, overprov=0.9),
    dict(id="C16", name="ServPro Facilities", industry="Servicos",
         size="Pequena", emp_count=160, software=False, intensity=0.06, start_m=9, growth=False, overprov=3.5),
    dict(id="C17", name="Ideia Fresca Marketing", industry="Marketing",
         size="Pequena", emp_count=75, software=False, intensity=0.80, start_m=0, growth=True, overprov=0.8),
    dict(id="C18", name="Estrela Marketing Digital", industry="Marketing",
         size="Media", emp_count=205, software=False, intensity=0.55, start_m=1, growth=True, overprov=1.0),
]

COMPANIES = {c["id"]: c for c in COMPANIES_RAW}

# --------------------------------------------------------------------------
# 2. DEPARTAMENTOS (simula estrutura organizacional de RH)
# (nome, peso_de_uso_de_ia [somente p/ geracao], peso_de_headcount)
# --------------------------------------------------------------------------

DEPT_TEMPLATES = {
    "Tecnologia / Desenvolvimento de Software": [
        ("Engenharia de Software", 0.90, 0.30), ("DevOps e Infraestrutura", 0.75, 0.08),
        ("Produto", 0.70, 0.08), ("Qualidade (QA)", 0.60, 0.10),
        ("Dados e Analytics", 0.65, 0.07), ("Vendas", 0.35, 0.10),
        ("Marketing", 0.50, 0.05), ("Suporte ao Cliente", 0.40, 0.12),
        ("RH", 0.20, 0.04), ("Financeiro", 0.20, 0.04), ("Juridico", 0.15, 0.02),
    ],
    "Financeiro": [
        ("Operacoes Bancarias", 0.25, 0.30), ("Risco e Compliance", 0.20, 0.10),
        ("Tecnologia da Informacao", 0.55, 0.12), ("Atendimento ao Cliente", 0.30, 0.20),
        ("Comercial", 0.25, 0.12), ("Financeiro", 0.20, 0.08),
        ("RH", 0.15, 0.05), ("Juridico", 0.15, 0.03),
    ],
    "Industria": [
        ("Producao", 0.05, 0.35), ("Manutencao", 0.08, 0.12), ("Logistica", 0.10, 0.12),
        ("Qualidade", 0.10, 0.08), ("Compras", 0.15, 0.06), ("Comercial", 0.20, 0.10),
        ("Financeiro", 0.15, 0.06), ("RH", 0.10, 0.05), ("Tecnologia da Informacao", 0.40, 0.06),
    ],
    "Varejo": [
        ("Operacoes de Loja", 0.08, 0.35), ("Logistica e Supply Chain", 0.15, 0.12),
        ("Comercial e Compras", 0.25, 0.10), ("Marketing", 0.45, 0.06),
        ("E-commerce", 0.55, 0.08), ("Atendimento ao Cliente", 0.30, 0.15),
        ("Financeiro", 0.15, 0.06), ("RH", 0.10, 0.04), ("Tecnologia da Informacao", 0.40, 0.04),
    ],
    "Servicos": [
        ("Operacoes", 0.35, 0.30), ("Comercial", 0.30, 0.15), ("Atendimento ao Cliente", 0.30, 0.15),
        ("Financeiro", 0.20, 0.08), ("RH", 0.15, 0.05), ("Tecnologia da Informacao", 0.45, 0.08),
        ("Consultoria e Projetos", 0.60, 0.19),
    ],
    "Marketing": [
        ("Criacao", 0.70, 0.25), ("Midia e Performance", 0.75, 0.25),
        ("Atendimento a Clientes (Account)", 0.50, 0.20), ("Planejamento e Estrategia", 0.55, 0.15),
        ("Financeiro", 0.20, 0.05), ("RH", 0.15, 0.04), ("Tecnologia da Informacao", 0.35, 0.06),
    ],
}

ROLE_POOL = {
    "Engenharia de Software": ["Desenvolvedor(a) Backend", "Desenvolvedor(a) Frontend", "Desenvolvedor(a) Full Stack", "Engenheiro(a) de Software"],
    "DevOps e Infraestrutura": ["Engenheiro(a) DevOps", "Engenheiro(a) de Infraestrutura", "Site Reliability Engineer"],
    "Produto": ["Product Manager", "Product Owner", "Analista de Produto"],
    "Qualidade (QA)": ["Analista de QA", "Engenheiro(a) de Testes"],
    "Dados e Analytics": ["Analista de Dados", "Engenheiro(a) de Dados", "Cientista de Dados"],
    "Tecnologia da Informacao": ["Analista de TI", "Administrador(a) de Sistemas", "Suporte Tecnico", "Analista de Infraestrutura"],
    "Vendas": ["Executivo(a) de Vendas", "Representante Comercial", "Coordenador(a) de Vendas"],
    "Comercial": ["Executivo(a) de Contas", "Analista Comercial", "Coordenador(a) Comercial"],
    "Comercial e Compras": ["Comprador(a)", "Analista de Compras", "Analista Comercial"],
    "Marketing": ["Analista de Marketing", "Coordenador(a) de Marketing", "Especialista em Midias Digitais"],
    "Suporte ao Cliente": ["Analista de Suporte", "Especialista em Atendimento"],
    "Atendimento ao Cliente": ["Analista de Atendimento", "Especialista em Relacionamento com Cliente"],
    "RH": ["Analista de RH", "Business Partner de RH", "Recrutador(a)"],
    "Financeiro": ["Analista Financeiro", "Controller Junior", "Analista de Contas a Pagar"],
    "Juridico": ["Analista Juridico", "Advogado(a) Corporativo"],
    "Operacoes Bancarias": ["Analista de Operacoes", "Especialista em Produtos Bancarios"],
    "Risco e Compliance": ["Analista de Risco", "Analista de Compliance"],
    "Producao": ["Operador(a) de Producao", "Supervisor(a) de Producao", "Tecnico(a) de Producao"],
    "Manutencao": ["Tecnico(a) de Manutencao", "Engenheiro(a) de Manutencao"],
    "Logistica": ["Analista de Logistica", "Coordenador(a) de Logistica"],
    "Logistica e Supply Chain": ["Analista de Supply Chain", "Coordenador(a) de Logistica"],
    "Qualidade": ["Analista de Qualidade", "Engenheiro(a) de Qualidade"],
    "Compras": ["Analista de Compras", "Comprador(a) Tecnico(a)"],
    "Operacoes de Loja": ["Gerente de Loja", "Vendedor(a)", "Operador(a) de Caixa"],
    "E-commerce": ["Analista de E-commerce", "Especialista em Marketplace"],
    "Operacoes": ["Analista de Operacoes", "Coordenador(a) de Operacoes"],
    "Consultoria e Projetos": ["Consultor(a)", "Gerente de Projetos", "Analista de Consultoria"],
    "Criacao": ["Redator(a) Publicitario(a)", "Diretor(a) de Arte", "Designer Grafico"],
    "Midia e Performance": ["Analista de Midia Paga", "Especialista em Performance"],
    "Atendimento a Clientes (Account)": ["Account Manager", "Analista de Contas"],
    "Planejamento e Estrategia": ["Planejador(a) Estrategico(a)", "Analista de Planejamento"],
}

SENIORITIES = ["Junior", "Pleno", "Senior", "Especialista", "Gerente"]
SENIORITY_W = [0.28, 0.35, 0.24, 0.08, 0.05]

SIZE_EMP_RANGE = {"Pequena": (25, 50), "Media": (70, 150), "Grande": (200, 380)}

# --------------------------------------------------------------------------
# 3. CATALOGO DE FERRAMENTAS E MODELOS DE IA
# (simula um inventario de software / CMDB corporativo)
# --------------------------------------------------------------------------

AI_TOOLS = [
    ("T01", "ChatGPT Enterprise", "OpenAI", "Assistente Generativo", "Por usuario (assinatura)"),
    ("T02", "Claude for Work", "Anthropic", "Assistente Generativo", "Por usuario (assinatura)"),
    ("T03", "Gemini for Google Workspace", "Google", "Assistente Generativo", "Por usuario (assinatura)"),
    ("T04", "Microsoft Copilot Chat", "Microsoft", "Assistente Generativo", "Por usuario (assinatura)"),
    ("T05", "GitHub Copilot", "GitHub / Microsoft", "Copiloto de Desenvolvimento", "Por usuario (assinatura)"),
    ("T06", "Amazon Q Developer", "Amazon Web Services", "Copiloto de Desenvolvimento", "Por usuario (assinatura)"),
    ("T07", "Tabnine", "Tabnine", "Copiloto de Desenvolvimento", "Por usuario (assinatura)"),
    ("T08", "Microsoft 365 Copilot", "Microsoft", "IA Integrada a Produtividade", "Por usuario (assinatura)"),
    ("T09", "Notion AI", "Notion Labs", "IA Integrada a Produtividade", "Por usuario (assinatura)"),
    ("T10", "Azure OpenAI Service", "Microsoft Azure", "Plataforma Corporativa de IA", "Consumo (pay-as-you-go)"),
    ("T11", "AWS Bedrock", "Amazon Web Services", "Plataforma Corporativa de IA", "Consumo (pay-as-you-go)"),
    ("T12", "Google Vertex AI", "Google Cloud", "Plataforma Corporativa de IA", "Consumo (pay-as-you-go)"),
    ("T13", "OpenAI API", "OpenAI", "API de Modelo", "Consumo (pay-as-you-go)"),
    ("T14", "Anthropic API", "Anthropic", "API de Modelo", "Consumo (pay-as-you-go)"),
    ("T15", "Zendesk Answer Bot AI", "Zendesk", "IA Integrada a Produtividade", "Por usuario (assinatura)"),
]
AI_TOOLS_DF = pd.DataFrame(AI_TOOLS, columns=["ai_tool_id", "tool_name", "provider", "tool_category", "licensing_type"])
TOOL_CATEGORY = {t[0]: t[3] for t in AI_TOOLS}

# precos SINTETICOS, em USD por 1.000 tokens -- nao refletem precos reais de mercado
AI_MODELS = [
    ("M01", "T01", "gpt-4o", "GPT", 0.005, 0.015),
    ("M02", "T01", "gpt-4o-mini", "GPT", 0.00015, 0.0006),
    ("M03", "T02", "claude-opus-4", "Claude", 0.015, 0.075),
    ("M04", "T02", "claude-sonnet-4", "Claude", 0.003, 0.015),
    ("M05", "T03", "gemini-1.5-pro", "Gemini", 0.00125, 0.005),
    ("M06", "T03", "gemini-1.5-flash", "Gemini", 0.000075, 0.0003),
    ("M07", "T04", "gpt-4o-copilot", "GPT", 0.005, 0.015),
    ("M08", "T05", "copilot-codex", "Codex", None, None),
    ("M09", "T06", "amazon-q-dev-model", "Amazon Titan", None, None),
    ("M10", "T07", "tabnine-base", "Proprietario", None, None),
    ("M11", "T08", "gpt-4o-m365", "GPT", 0.005, 0.015),
    ("M12", "T09", "notion-ai-model", "Proprietario", None, None),
    ("M13", "T10", "gpt-4o", "GPT", 0.005, 0.015),
    ("M14", "T10", "gpt-35-turbo", "GPT", 0.0005, 0.0015),
    ("M15", "T11", "anthropic.claude-3-sonnet", "Claude", 0.003, 0.015),
    ("M16", "T11", "amazon.titan-text-premier", "Amazon Titan", 0.0005, 0.0015),
    ("M17", "T12", "gemini-1.5-pro", "Gemini", 0.00125, 0.005),
    ("M18", "T12", "text-bison", "PaLM", 0.0005, 0.0015),
    ("M19", "T13", "gpt-4o", "GPT", 0.005, 0.015),
    ("M20", "T13", "gpt-4o-mini", "GPT", 0.00015, 0.0006),
    ("M21", "T13", "gpt-3.5-turbo", "GPT", 0.0005, 0.0015),
    ("M22", "T14", "claude-opus-4", "Claude", 0.015, 0.075),
    ("M23", "T14", "claude-sonnet-4", "Claude", 0.003, 0.015),
    ("M24", "T14", "claude-haiku-3.5", "Claude", 0.0008, 0.004),
    ("M25", "T15", "zendesk-ai-base", "Proprietario", None, None),
]
AI_MODELS_DF = pd.DataFrame(AI_MODELS, columns=["model_id", "ai_tool_id", "model_name", "model_family", "input_price_usd_per_1k_tokens", "output_price_usd_per_1k_tokens"])
MODELS_BY_TOOL = {}
for m in AI_MODELS:
    MODELS_BY_TOOL.setdefault(m[1], []).append(m)

TOOL_DISCONTINUED = "T15"          # ferramenta que sai de uso ao longo do periodo
TOOL_DISCONTINUED_LAST_MONTH = 3   # ultimo indice de mes em que ainda aparece em uso

APPLICATIONS_BY_CATEGORY = {
    "Assistente Generativo": ["Chat Web", "App Mobile", "Slack Bot", "Microsoft Teams App"],
    "Copiloto de Desenvolvimento": ["Extensao VSCode", "IDE Plugin - JetBrains", "CLI", "Extensao Vim"],
    "IA Integrada a Produtividade": ["Add-in Outlook", "Add-in Word", "Add-in Excel", "Add-in PowerPoint", "Microsoft Teams App"],
    "Plataforma Corporativa de IA": ["API Direta", "Console de Administracao", "Pipeline Interno"],
    "API de Modelo": ["API Direta", "Integracao via Backend Interno"],
}

DEV_DEPTS = {"Engenharia de Software", "DevOps e Infraestrutura", "Produto", "Qualidade (QA)", "Dados e Analytics"}
IT_DATA_DEPTS = {"Tecnologia da Informacao", "Dados e Analytics"}
CREATIVE_DEPTS = {"Criacao", "Midia e Performance", "Marketing", "Planejamento e Estrategia"}

print("Configuracao carregada. Gerando tabelas...")

# --------------------------------------------------------------------------
# GERACAO: companies.csv
# --------------------------------------------------------------------------

companies_rows = []
for c in COMPANIES_RAW:
    companies_rows.append(dict(
        company_id=c["id"], company_name=c["name"], industry=c["industry"],
        company_size=c["size"], employee_count=c["emp_count"],
    ))
companies_df = pd.DataFrame(companies_rows)

# --------------------------------------------------------------------------
# GERACAO: departments.csv
# --------------------------------------------------------------------------

departments_rows = []
dept_lookup = {}   # company_id -> list of (department_id, name, usage_skew)
for c in COMPANIES_RAW:
    template = DEPT_TEMPLATES[c["industry"]]
    dept_lookup[c["id"]] = []
    for i, (dname, uskew, hweight) in enumerate(template, start=1):
        did = f"{c['id']}-D{i:02d}"
        departments_rows.append(dict(department_id=did, company_id=c["id"], department_name=dname))
        dept_lookup[c["id"]].append((did, dname, uskew, hweight))
departments_df = pd.DataFrame(departments_rows)

# --------------------------------------------------------------------------
# GERACAO: employees.csv
# (representa apenas colaboradores com acesso a sistemas corporativos /
#  potencialmente elegiveis a ferramentas de IA -- nao o headcount total,
#  que fica registrado em companies.employee_count)
# --------------------------------------------------------------------------

employees_rows = []
employee_activity = {}   # employee_id -> classe interna de atividade (nao exportada)
employee_dept_info = {}  # employee_id -> (company_id, department_id, department_name)
employees_by_company_dept = {}  # (company_id, department_id) -> list[employee_id]

emp_counter = 0
for c in COMPANIES_RAW:
    cid = c["id"]
    n_lo, n_hi = SIZE_EMP_RANGE[c["size"]]
    n_employees = random.randint(n_lo, n_hi)
    depts = dept_lookup[cid]
    weights = np.array([d[3] for d in depts], dtype=float)
    weights = weights / weights.sum()
    dept_assignment = np.random.choice(len(depts), size=n_employees, p=weights)

    for j in range(n_employees):
        emp_counter += 1
        did, dname, uskew, _ = depts[dept_assignment[j]]
        employee_id = f"{cid}-E{j+1:04d}"

        role_pool = ROLE_POOL.get(dname, ["Analista"])
        role = random.choice(role_pool)
        seniority = np.random.choice(SENIORITIES, p=SENIORITY_W)

        # gerentes tendem a ter mais tempo de casa
        min_days = 60 if seniority in ("Junior", "Pleno") else 180
        days_back = random.randint(min_days, 6 * 365)
        hire_date = TODAY - timedelta(days=days_back)

        active = np.random.choice([True, False], p=[0.92, 0.08])
        cost_center = f"CC-{cid}-{did.split('-')[-1]}"
        if random.random() < 0.02:
            cost_center = None  # pequena inconsistencia de cadastro

        employees_rows.append(dict(
            employee_id=employee_id, company_id=cid, department_id=did,
            job_role=role, seniority=seniority, hire_date=hire_date.isoformat(),
            cost_center=cost_center, active=bool(active),
        ))

        # --- classificacao interna de atividade (NAO exportada) ---
        score = clip(c["intensity"] * 0.55 + uskew * 0.45 + np.random.normal(0, 0.08), 0.0, 1.0)
        if not active:
            score *= 0.05
        w_never = clip(0.92 - score, 0.04, 0.92)
        remaining = 1 - w_never
        w_heavy = remaining * clip(score, 0, 1) * 0.75
        w_moderate = remaining * 0.45
        w_light = remaining - w_heavy - w_moderate
        if w_light < 0:
            w_light = 0.0
        raw_w = np.array([w_never, w_light, w_moderate, w_heavy])
        raw_w = np.clip(raw_w, 0, None)
        raw_w = raw_w / raw_w.sum()
        activity_class = np.random.choice(["never", "light", "moderate", "heavy"], p=raw_w)

        employee_activity[employee_id] = dict(activity_class=activity_class, score=score, dept_name=dname, dept_usage_skew=uskew)
        employee_dept_info[employee_id] = (cid, did, dname)
        employees_by_company_dept.setdefault((cid, did), []).append(employee_id)

employees_df = pd.DataFrame(employees_rows)
print(f"  employees: {len(employees_df)} linhas")

# --------------------------------------------------------------------------
# FERRAMENTAS ADOTADAS POR EMPRESA (uso interno para gerar licencas,
# aprovacoes e logs de forma consistente entre tabelas)
# --------------------------------------------------------------------------

GEN_ASSISTANTS = ["T01", "T02", "T03", "T04"]
PRODUCTIVITY = ["T04", "T08", "T09"]
DEV_COPILOTS = ["T05", "T06", "T07"]
PLATFORMS = ["T10", "T11", "T12"]
APIS = ["T13", "T14"]

adopted_tools = {}  # company_id -> set(ai_tool_id)
for c in COMPANIES_RAW:
    cid = c["id"]
    tools = set()
    tools.add(random.choice(["T01", "T02", "T03"]))
    tools.add(random.choice(["T04", "T08"]))
    if c["software"]:
        tools.add("T05")
        if c["intensity"] > 0.5:
            tools.add(random.choice(["T06", "T07"]))
    if c["intensity"] > 0.45:
        tools.add(random.choice(APIS))
        tools.add("T09")
    if c["intensity"] > 0.6 and (c["software"] or c["industry"] in ("Servicos", "Marketing")):
        tools.add(random.choice(PLATFORMS))
    if c["industry"] in ("Varejo", "Servicos") and random.random() < 0.5:
        tools.add(TOOL_DISCONTINUED)  # ferramenta legada, sera descontinuada
    if c["intensity"] < 0.2 and random.random() < 0.5:
        # empresas muito conservadoras as vezes tem so 1-2 ferramentas
        tools = set(list(tools)[:2]) if len(tools) > 2 else tools
    adopted_tools[cid] = tools

# --------------------------------------------------------------------------
# GERACAO: ai_licenses.csv
# (simula extrato de um painel de administracao de licencas SaaS)
# --------------------------------------------------------------------------

licenses_rows = []
license_holders = {}  # (company_id, ai_tool_id) -> set(employee_id) com licenca ATIVA

for c in COMPANIES_RAW:
    cid = c["id"]
    company_employees = [e for e in employees_df[employees_df.company_id == cid]["employee_id"].tolist()]
    for tool_id in adopted_tools[cid]:
        category = TOOL_CATEGORY[tool_id]
        # publico potencial da ferramenta, por categoria
        if category == "Copiloto de Desenvolvimento":
            potential = [e for e in company_employees if employee_dept_info[e][2] in DEV_DEPTS]
        elif category == "Plataforma Corporativa de IA" or category == "API de Modelo":
            potential = [e for e in company_employees if employee_dept_info[e][2] in IT_DATA_DEPTS]
        else:
            potential = company_employees

        if not potential:
            potential = company_employees
        if not potential:
            continue

        overprov = COMPANIES[cid]["overprov"]
        n_licenses = int(clip(round(len(potential) * min(overprov, 3.0) * random.uniform(0.5, 1.0)), 1, len(company_employees)))
        n_licenses = min(n_licenses, len(company_employees))
        holders = list(np.random.choice(company_employees, size=n_licenses, replace=False)) if n_licenses > 0 else []

        start_m = COMPANIES[cid]["start_m"]
        if tool_id == TOOL_DISCONTINUED:
            assign_period = MONTHS[0]
        else:
            assign_period = MONTHS[min(start_m, N_MONTHS - 1)]

        active_holders = set()
        for h in holders:
            assigned_date = rand_date_in_month(assign_period) + timedelta(days=random.randint(0, 45))
            if assigned_date > TODAY:
                assigned_date = TODAY - timedelta(days=random.randint(1, 30))

            if tool_id == TOOL_DISCONTINUED:
                expiration_date = assigned_date + timedelta(days=random.randint(90, 150))
                status_choices = ["Expirada", "Revogada"]
                status_w = [0.75, 0.25]
            else:
                perpetual = random.random() < 0.30
                expiration_date = None if perpetual else assigned_date + timedelta(days=365)
                if expiration_date is not None and expiration_date < TODAY:
                    status_choices = ["Expirada", "Ativa"]
                    status_w = [0.6, 0.4]
                else:
                    status_choices = ["Ativa", "Revogada"]
                    status_w = [0.93, 0.07]
            license_status = np.random.choice(status_choices, p=status_w)
            if license_status == "Ativa":
                active_holders.add(h)

            license_type = "Enterprise" if category in ("Plataforma Corporativa de IA", "API de Modelo") else random.choice(["Standard", "Professional", "Enterprise"])

            licenses_rows.append(dict(
                company_id=cid, employee_id=h, ai_tool_id=tool_id,
                license_type=license_type,
                assigned_date=assigned_date.isoformat(),
                expiration_date=expiration_date.isoformat() if expiration_date else None,
                license_status=license_status,
            ))
        license_holders[(cid, tool_id)] = active_holders

licenses_df = pd.DataFrame(licenses_rows)
print(f"  ai_licenses: {len(licenses_df)} linhas")

# --------------------------------------------------------------------------
# GERACAO: approved_ai_tools.csv
# (simula registro de comite de governanca / seguranca da informacao)
# --------------------------------------------------------------------------

approved_rows = []
for c in COMPANIES_RAW:
    cid = c["id"]
    conservative = c["industry"] in ("Financeiro", "Industria")
    all_dept_names = [d[1] for d in dept_lookup[cid]]
    considered = set(adopted_tools[cid])
    # empresas tambem avaliam (e as vezes bloqueiam) 1-2 ferramentas que nao chegam a adotar
    extra_candidates = [t for t in AI_TOOLS_DF.ai_tool_id if t not in considered]
    considered.update(random.sample(extra_candidates, k=min(2, len(extra_candidates))))

    for tool_id in considered:
        if tool_id in adopted_tools[cid]:
            status_w = [0.85, 0.05, 0.10] if not conservative else [0.65, 0.15, 0.20]
        else:
            status_w = [0.05, 0.55, 0.40] if not conservative else [0.02, 0.75, 0.23]
        approval_status = np.random.choice(["Aprovado", "Bloqueado", "Em Avaliacao"], p=status_w)

        approval_date = rand_date_in_month(random.choice(MONTHS[:6])).isoformat()

        if approval_status != "Aprovado":
            allowed_departments = None
        else:
            category = TOOL_CATEGORY[tool_id]
            if category == "Copiloto de Desenvolvimento":
                allowed = [d for d in all_dept_names if d in DEV_DEPTS]
            elif category in ("Plataforma Corporativa de IA", "API de Modelo"):
                allowed = [d for d in all_dept_names if d in IT_DATA_DEPTS]
            else:
                allowed = all_dept_names
            if not allowed or random.random() < 0.3:
                allowed_departments = "Todos os departamentos"
            else:
                # pequena inconsistencia de formatacao proposital (as vezes com espacos extras)
                sep = "; " if random.random() < 0.2 else ", "
                allowed_departments = sep.join(allowed)

        approved_rows.append(dict(
            company_id=cid, ai_tool_id=tool_id, approval_status=approval_status,
            approval_date=approval_date, allowed_departments=allowed_departments,
        ))

approved_ai_tools_df = pd.DataFrame(approved_rows)
print(f"  approved_ai_tools: {len(approved_ai_tools_df)} linhas")

# --------------------------------------------------------------------------
# GERACAO: projects.csv (apenas empresas de desenvolvimento de software)
# (simula cadastro de projetos em uma ferramenta tipo Jira/Azure DevOps)
# --------------------------------------------------------------------------

PROJECT_TYPES = ["Produto Interno", "Cliente", "Plataforma", "Mobile", "API/Integracao"]
PROJECT_NAME_WORDS = ["Atlas", "Orion", "Nimbus", "Vertex", "Helios", "Zenith", "Quartz",
                       "Falcon", "Prisma", "Aurora", "Cobalt", "Titan", "Nexus", "Polaris"]

projects_rows = []
projects_by_company = {}
software_companies = [c for c in COMPANIES_RAW if c["software"]]
for c in COMPANIES_RAW:
    if not c["software"]:
        continue
    cid = c["id"]
    n_projects = random.randint(2, 5)
    projects_by_company[cid] = []
    used_names = set()
    for i in range(1, n_projects + 1):
        pid = f"{cid}-P{i:02d}"
        while True:
            pname = f"Projeto {random.choice(PROJECT_NAME_WORDS)}"
            if pname not in used_names:
                used_names.add(pname)
                break
        ptype = random.choice(PROJECT_TYPES)
        days_back = random.randint(120, 900)
        start_date = TODAY - timedelta(days=days_back)
        status = np.random.choice(["Ativo", "Concluido", "Pausado"], p=[0.65, 0.22, 0.13])
        projects_rows.append(dict(
            project_id=pid, company_id=cid, project_name=pname, project_type=ptype,
            start_date=start_date.isoformat(), status=status,
        ))
        projects_by_company[cid].append((pid, start_date, status))

projects_df = pd.DataFrame(projects_rows)
print(f"  projects: {len(projects_df)} linhas")

# --------------------------------------------------------------------------
# GERACAO: ai_usage_logs.csv
# (simula logs de um gateway/proxy corporativo de IA -- sem conteudo de
#  prompts e sem dados pessoais, apenas metadados tecnicos de uso)
# --------------------------------------------------------------------------

REQUEST_COUNT_RANGE = {
    "Copiloto de Desenvolvimento": (5, 150),
    "Assistente Generativo": (1, 40),
    "IA Integrada a Produtividade": (1, 25),
    "Plataforma Corporativa de IA": (1, 200),
    "API de Modelo": (1, 300),
}
TOKENS_PER_REQUEST_RANGE = {
    "Copiloto de Desenvolvimento": ((30, 300), (20, 200)),
    "Assistente Generativo": ((100, 1500), (150, 2000)),
    "IA Integrada a Produtividade": ((50, 800), (50, 800)),
    "Plataforma Corporativa de IA": ((50, 2000), (50, 2000)),
    "API de Modelo": ((50, 2000), (50, 2000)),
}
CLASS_SCALE = {"light": 0.22, "moderate": 0.55, "heavy": 1.0}
CLASS_LAMBDA = {"light": (1, 4), "moderate": (5, 18), "heavy": (20, 55)}


def get_available_tools(cid, dept_name, month_idx, is_software):
    tools = adopted_tools[cid]
    avail, weights = [], []
    for t in tools:
        if t == TOOL_DISCONTINUED and month_idx > TOOL_DISCONTINUED_LAST_MONTH:
            continue
        cat = TOOL_CATEGORY[t]
        if cat == "Copiloto de Desenvolvimento":
            w = 3.0 if (is_software and dept_name in DEV_DEPTS) else (0.3 if dept_name in IT_DATA_DEPTS else 0.02)
        elif cat == "Assistente Generativo":
            w = 2.0 if dept_name in CREATIVE_DEPTS else 1.3
        elif cat == "IA Integrada a Produtividade":
            w = 1.5
        else:  # Plataforma Corporativa de IA / API de Modelo
            w = 2.0 if dept_name in IT_DATA_DEPTS else 0.05
        if t == TOOL_DISCONTINUED:
            w *= 0.6
        if w > 0:
            avail.append(t)
            weights.append(w)
    if not avail:
        return [], []
    s = sum(weights)
    return avail, [w / s for w in weights]


def pick_model(tool_id):
    models = MODELS_BY_TOOL[tool_id]
    if len(models) == 1:
        return models[0]
    n = len(models)
    ranks = np.arange(n, 0, -1).astype(float)
    weights = ranks / ranks.sum()
    idx = np.random.choice(n, p=weights)
    return models[idx]


usage_rows = []
usage_counter = 0

for cid, c in COMPANIES.items():
    company_start_m = c["start_m"]
    is_software = c["software"]
    growth = c["growth"]
    company_projects = projects_by_company.get(cid, [])

    company_emp_ids = employees_df[employees_df.company_id == cid]["employee_id"].tolist()

    for employee_id in company_emp_ids:
        info = employee_activity[employee_id]
        activity_class = info["activity_class"]
        if activity_class == "never":
            continue

        dept_cid, dept_id, dept_name = employee_dept_info[employee_id]
        hire_date_str = employees_df.loc[employees_df.employee_id == employee_id, "hire_date"].values[0]
        hire_date = pd.Timestamp(hire_date_str).date()
        emp_cost_center = employees_df.loc[employees_df.employee_id == employee_id, "cost_center"].values[0]

        lam_lo, lam_hi = CLASS_LAMBDA[activity_class]
        base_lambda = random.uniform(lam_lo, lam_hi)

        for m_idx, period in enumerate(MONTHS):
            if m_idx < company_start_m:
                continue
            m_start, m_end = month_bounds(period)
            if hire_date > m_end:
                continue

            if growth:
                span = max(N_MONTHS - 1 - company_start_m, 1)
                progress = (m_idx - company_start_m) / span
                growth_mult = 0.6 + 0.9 * progress
            else:
                growth_mult = 1.0

            season_mult = 1.0
            if period.month == 12:
                season_mult = 0.55
            elif period.month == 1:
                season_mult = 0.8
            elif period.month == 7:
                season_mult = 0.85

            noise_mult = clip(np.random.normal(1.0, 0.18), 0.25, 1.9)
            lam = max(base_lambda * growth_mult * season_mult * noise_mult, 0.05)
            n_sessions = np.random.poisson(lam)
            if n_sessions <= 0:
                continue

            avail_tools, tool_weights = get_available_tools(cid, dept_name, m_idx, is_software)
            if not avail_tools:
                continue

            for _ in range(n_sessions):
                tool_id = np.random.choice(avail_tools, p=tool_weights)
                cat = TOOL_CATEGORY[tool_id]
                model_row = pick_model(tool_id)
                model_id, model_family = model_row[0], model_row[3]

                usage_counter += 1
                usage_id = f"U{usage_counter:07d}"

                ts = rand_datetime_in_month(period)
                # nao permitir timestamp antes da contratacao
                if ts.date() < hire_date:
                    ts = pd.Timestamp(hire_date) + pd.Timedelta(hours=random.randint(0, 23))

                lo, hi = REQUEST_COUNT_RANGE[cat]
                scale = CLASS_SCALE[activity_class]
                hi_scaled = max(lo + 1, int(hi * scale))
                request_count = random.randint(lo, hi_scaled)

                (in_lo, in_hi), (out_lo, out_hi) = TOKENS_PER_REQUEST_RANGE[cat]
                input_tokens = int(request_count * random.randint(in_lo, in_hi) * random.uniform(0.7, 1.3))
                output_tokens = int(request_count * random.randint(out_lo, out_hi) * random.uniform(0.6, 1.2))

                if cat == "Copiloto de Desenvolvimento":
                    latency_ms = int(clip(np.random.normal(320, 130), 60, 1600))
                elif model_family in ("Claude", "GPT") and "haiku" not in str(model_id).lower() and "mini" not in str(model_id).lower():
                    latency_ms = int(clip(np.random.normal(2100, 800), 300, 7000))
                else:
                    latency_ms = int(clip(np.random.normal(650, 280), 150, 4000))

                request_status = np.random.choice(
                    ["success", "error", "timeout", "rate_limited"],
                    p=[0.95, 0.02, 0.02, 0.01],
                )
                if request_status != "success" and random.random() < 0.4:
                    latency_ms = None  # telemetria de latencia as vezes nao e capturada em falhas

                project_id = None
                if cat == "Copiloto de Desenvolvimento" and is_software and dept_name in DEV_DEPTS and company_projects:
                    active_projects = [p for p in company_projects if p[1] <= m_end]
                    if active_projects:
                        project_id = random.choice(active_projects)[0]

                row_cost_center = emp_cost_center
                if row_cost_center is not None and random.random() < 0.01:
                    row_cost_center = None

                application = random.choice(APPLICATIONS_BY_CATEGORY[cat])

                usage_rows.append(dict(
                    usage_id=usage_id, timestamp=ts.isoformat(sep=" "),
                    employee_id=employee_id, company_id=cid, department_id=dept_id,
                    ai_tool_id=tool_id, model_id=model_id, application=application,
                    request_count=request_count, input_tokens=input_tokens, output_tokens=output_tokens,
                    latency_ms=latency_ms, request_status=request_status,
                    project_id=project_id, cost_center=row_cost_center,
                ))

    print(f"  ai_usage_logs (acumulado apos {cid}): {len(usage_rows)} linhas")

usage_logs_df = pd.DataFrame(usage_rows)
print(f"  ai_usage_logs TOTAL: {len(usage_logs_df)} linhas")

# --------------------------------------------------------------------------
# GERACAO: ai_billing.csv
# (simula extrato de fatura de provedor / relatorio FinOps mensal)
# Ferramentas "Consumo (pay-as-you-go)" sao faturadas por token (USD);
# ferramentas "Por usuario (assinatura)" sao faturadas por licenca ativa (BRL).
# A mistura de moedas e PROPOSITAL: reflete a realidade de faturas de
# provedores internacionais (USD) vs. contratos locais de licenciamento (BRL).
# --------------------------------------------------------------------------

billing_rows = []
billing_counter = 0

usage_logs_df["_ts"] = pd.to_datetime(usage_logs_df["timestamp"])
usage_logs_df["_month"] = usage_logs_df["_ts"].dt.to_period("M")

price_lookup = {m[0]: (m[4], m[5]) for m in AI_MODELS}  # model_id -> (input_price, output_price)
consumption_tools = set(AI_TOOLS_DF[AI_TOOLS_DF.licensing_type == "Consumo (pay-as-you-go)"]["ai_tool_id"])
per_seat_tools = set(AI_TOOLS_DF[AI_TOOLS_DF.licensing_type == "Por usuario (assinatura)"]["ai_tool_id"])

# --- faturamento por consumo (tokens) ---
cons_df = usage_logs_df[usage_logs_df.ai_tool_id.isin(consumption_tools)]
grp = cons_df.groupby(["company_id", "_month", "ai_tool_id", "model_id", "cost_center"], dropna=False).agg(
    input_tokens=("input_tokens", "sum"), output_tokens=("output_tokens", "sum")
).reset_index()

for _, r in grp.iterrows():
    if random.random() < 0.04:
        continue  # fatura ausente naquele mes (falha de integracao / atraso do provedor)
    in_price, out_price = price_lookup.get(r["model_id"], (None, None))
    if in_price is None:
        continue
    total_tokens = r["input_tokens"] + r["output_tokens"]
    raw_cost = (r["input_tokens"] / 1000.0) * in_price + (r["output_tokens"] / 1000.0) * out_price
    invoice_noise = random.uniform(0.96, 1.05)
    billing_counter += 1
    billing_rows.append(dict(
        billing_id=f"B{billing_counter:06d}", company_id=r["company_id"],
        billing_month=str(r["_month"]), provider=AI_TOOLS_DF.set_index("ai_tool_id").loc[r["ai_tool_id"], "provider"],
        ai_tool_id=r["ai_tool_id"], model_id=r["model_id"],
        cost_center=r["cost_center"] if pd.notna(r["cost_center"]) else None,
        usage_quantity=int(total_tokens), total_cost=round(raw_cost * invoice_noise, 2),
        currency="USD",
    ))

# --- faturamento por licenca (assinatura por usuario) ---
licenses_df["_assigned"] = pd.to_datetime(licenses_df["assigned_date"])
licenses_df["_expiration"] = pd.to_datetime(licenses_df["expiration_date"])

seat_price_base = {t: random.uniform(60, 220) for t in per_seat_tools}

for cid in COMPANIES:
    comp_licenses = licenses_df[(licenses_df.company_id == cid) & (licenses_df.ai_tool_id.isin(per_seat_tools))]
    if comp_licenses.empty:
        continue
    for tool_id in comp_licenses.ai_tool_id.unique():
        tool_licenses = comp_licenses[comp_licenses.ai_tool_id == tool_id]
        # junta cost_center via employees
        tool_licenses = tool_licenses.merge(employees_df[["employee_id", "cost_center"]], on="employee_id", how="left", suffixes=("", "_emp"))
        for m_idx, period in enumerate(MONTHS):
            m_start, m_end = month_bounds(period)
            active_mask = (
                (tool_licenses["_assigned"].dt.date <= m_end)
                & (tool_licenses["license_status"] == "Ativa")
                & (tool_licenses["_expiration"].isna() | (tool_licenses["_expiration"].dt.date >= m_start))
            )
            active = tool_licenses[active_mask]
            if active.empty:
                continue
            if random.random() < 0.03:
                continue  # fatura ausente
            inflation = 1 + 0.004 * m_idx
            for cc, sub in active.groupby(active["cost_center"].fillna("SEM_CC")):
                n = len(sub)
                unit_price = seat_price_base[tool_id] * inflation * random.uniform(0.97, 1.03)
                billing_counter += 1
                billing_rows.append(dict(
                    billing_id=f"B{billing_counter:06d}", company_id=cid, billing_month=str(period),
                    provider=AI_TOOLS_DF.set_index("ai_tool_id").loc[tool_id, "provider"],
                    ai_tool_id=tool_id, model_id=None,
                    cost_center=None if cc == "SEM_CC" else cc,
                    usage_quantity=n, total_cost=round(n * unit_price, 2), currency="BRL",
                ))

billing_df = pd.DataFrame(billing_rows)
usage_logs_df.drop(columns=["_ts", "_month"], inplace=True)
print(f"  ai_billing: {len(billing_df)} linhas")

# --------------------------------------------------------------------------
# GERACAO: ai_budgets.csv
# (simula planejamento orcamentario por centro de custo, independente do gasto real)
# --------------------------------------------------------------------------

SIZE_BUDGET_MULT = {"Pequena": 0.5, "Media": 1.0, "Grande": 2.6}
budgets_rows = []
for c in COMPANIES_RAW:
    cid = c["id"]
    for did, dname, uskew, hweight in dept_lookup[cid]:
        cc = f"CC-{cid}-{did.split('-')[-1]}"
        base = random.uniform(1500, 9000) * SIZE_BUDGET_MULT[c["size"]] * (0.4 + hweight * 2.2)
        mid_year_bump = random.random() < 0.25
        for m_idx, period in enumerate(MONTHS):
            factor = random.uniform(0.85, 1.15)
            if mid_year_bump and m_idx >= 7:
                factor *= 1.2
            budgets_rows.append(dict(
                company_id=cid, cost_center=cc, year=period.year, month=period.month,
                budget_amount=round(base * factor, 2),
            ))
budgets_df = pd.DataFrame(budgets_rows)
print(f"  ai_budgets: {len(budgets_df)} linhas")

# --------------------------------------------------------------------------
# GERACAO: training_records.csv (simula extrato de LMS corporativo)
# --------------------------------------------------------------------------

COURSE_CATALOG = [
    ("Fundamentos de IA Generativa", "IA Generativa", 2.0),
    ("Prompt Engineering na Pratica", "IA Generativa", 3.0),
    ("Copilot para Desenvolvedores", "Ferramentas de Desenvolvimento", 4.0),
    ("Seguranca da Informacao e Uso de IA", "Seguranca e Compliance", 1.5),
    ("Etica e Governanca de Dados em IA", "Governanca", 2.0),
    ("Introducao ao ChatGPT Enterprise", "Ferramentas Corporativas", 1.0),
    ("Automatizacao de Tarefas com IA", "Produtividade", 2.5),
    ("LGPD e Ferramentas de IA", "Seguranca e Compliance", 2.0),
    ("IA para Times Comerciais", "Aplicacoes de Negocio", 1.5),
    ("Analise de Dados com IA", "Dados e Analytics", 4.0),
    ("Microsoft 365 Copilot na Pratica", "Ferramentas Corporativas", 2.0),
    ("IA Responsavel: Vieses e Limitacoes", "Governanca", 1.5),
]

training_rows = []
training_counter = 0
for cid, c in COMPANIES.items():
    participation_rate = clip(0.25 + c["intensity"] * 0.55 + random.uniform(-0.1, 0.1), 0.1, 0.9)
    company_emp_ids = employees_df[(employees_df.company_id == cid) & (employees_df.active == True)]["employee_id"].tolist()
    n_participants = int(len(company_emp_ids) * participation_rate)
    participants = random.sample(company_emp_ids, min(n_participants, len(company_emp_ids)))
    for emp in participants:
        n_courses = np.random.choice([1, 2, 3], p=[0.6, 0.3, 0.1])
        chosen_courses = random.sample(COURSE_CATALOG, k=min(n_courses, len(COURSE_CATALOG)))
        for course_name, course_category, base_duration in chosen_courses:
            enrollment_period = random.choice(MONTHS)
            enrollment_date = rand_date_in_month(enrollment_period)
            completion_status = np.random.choice(
                ["Concluido", "Em Andamento", "Nao Iniciado", "Abandonado"],
                p=[0.58, 0.19, 0.13, 0.10],
            )
            if completion_status == "Concluido":
                completion_date = enrollment_date + timedelta(days=random.randint(3, 60))
                if completion_date > TODAY:
                    completion_date = TODAY
                score = random.randint(55, 100)
            elif completion_status == "Abandonado":
                completion_date = None
                score = None
            else:
                completion_date = None
                score = None
            training_counter += 1
            duration_hours = round(base_duration * random.uniform(0.85, 1.2), 1)
            training_rows.append(dict(
                training_id=f"TR{training_counter:06d}", employee_id=emp, company_id=cid,
                course_name=course_name, course_category=course_category,
                enrollment_date=enrollment_date.isoformat(),
                completion_date=completion_date.isoformat() if completion_date else None,
                completion_status=completion_status, score=score, duration_hours=duration_hours,
            ))

training_df = pd.DataFrame(training_rows)
print(f"  training_records: {len(training_df)} linhas")

# --------------------------------------------------------------------------
# GERACAO: security_events.csv
# (simula log de ferramenta CASB/DLP -- poucos eventos, mas plausiveis)
# --------------------------------------------------------------------------

EVENT_TYPES = [
    "Bloqueio de Ferramenta Nao Autorizada",
    "Alerta de DLP - Possivel Dado Sensivel em Prompt",
    "Tentativa de Acesso Fora da Politica de Uso",
    "Uso de Ferramenta Nao Aprovada Detectado (Shadow AI)",
    "Upload de Arquivo Bloqueado por Politica",
]
SEVERITIES = ["Baixa", "Media", "Alta", "Critica"]
POLICIES = ["POL-DATA-001", "POL-ACCESS-002", "POL-VENDOR-003", "POL-DLP-004", "POL-USO-005"]
ACTIONS = ["Bloqueado", "Alertado - Sem Acao Automatica", "Escalado para Seguranca da Informacao", "Sessao Encerrada"]

security_rows = []
security_counter = 0
for cid, c in COMPANIES.items():
    conservative = c["industry"] in ("Financeiro", "Industria")
    n_events = random.randint(4, 9) if conservative else random.randint(1, 6)
    company_emp_ids = employees_df[employees_df.company_id == cid]["employee_id"].tolist()
    tools_in_play = list(adopted_tools[cid]) + [t for t in AI_TOOLS_DF.ai_tool_id if t not in adopted_tools[cid]][:2]
    for _ in range(n_events):
        security_counter += 1
        emp = random.choice(company_emp_ids)
        period = random.choice(MONTHS)
        ts = rand_datetime_in_month(period)
        event_type = random.choice(EVENT_TYPES)
        severity = np.random.choice(SEVERITIES, p=[0.35, 0.35, 0.22, 0.08])
        tool_id = random.choice(tools_in_play)
        policy_id = random.choice(POLICIES)
        if "Bloqueio" in event_type or "Bloqueado" in event_type:
            action_taken = "Bloqueado"
        else:
            action_taken = random.choice(ACTIONS)
        security_rows.append(dict(
            event_id=f"SEC{security_counter:05d}", company_id=cid, employee_id=emp,
            timestamp=ts.isoformat(sep=" "), ai_tool_id=tool_id, event_type=event_type,
            severity=severity, policy_id=policy_id, action_taken=action_taken,
        ))

security_events_df = pd.DataFrame(security_rows)
print(f"  security_events: {len(security_events_df)} linhas")

# --------------------------------------------------------------------------
# GERACAO: pull_requests.csv / issues.csv / builds.csv / deployments.csv
# (apenas empresas de desenvolvimento de software -- simula extratos de
#  GitHub/GitLab, Jira e um pipeline de CI/CD)
# Nao ha nenhum campo indicando se algo foi "gerado por IA": isso e
# deliberado, para permitir correlacao futura com ai_usage_logs.
# --------------------------------------------------------------------------

ISSUE_TYPES = ["Bug", "Feature", "Debito Tecnico", "Chore"]
ISSUE_PRIORITIES = ["Baixa", "Media", "Alta", "Critica"]
PR_STATUS = ["Merged", "Fechado sem merge", "Aberto"]
BUILD_STATUS = ["Sucesso", "Falha"]
DEPLOY_ENVS = ["Dev", "Staging", "Producao"]
DEPLOY_STATUS = ["Sucesso", "Falha", "Revertido"]

pr_rows, issue_rows, build_rows, deploy_rows = [], [], [], []
pr_counter = issue_counter = build_counter = deploy_counter = 0

for c in COMPANIES_RAW:
    if not c["software"]:
        continue
    cid = c["id"]
    dev_emps = [e for e in employees_df[employees_df.company_id == cid]["employee_id"]
                if employee_dept_info[e][2] in DEV_DEPTS]
    if not dev_emps:
        continue
    team_size = len(dev_emps)

    for pid, start_date, status in projects_by_company.get(cid, []):
        for period in MONTHS:
            m_start, m_end = month_bounds(period)
            if m_start < start_date:
                continue
            if status == "Pausado" and random.random() < 0.7:
                continue
            activity_scale = random.uniform(0.5, 1.3)
            if status == "Concluido" and m_start > (start_date + timedelta(days=500)):
                activity_scale *= 0.3  # projeto concluido ha tempo: pouca atividade residual

            # --- pull requests ---
            n_prs = np.random.poisson(clip(team_size * 0.5 * activity_scale, 0.3, 60))
            for _ in range(n_prs):
                pr_counter += 1
                emp = random.choice(dev_emps)
                created_at = rand_datetime_in_month(period)
                pr_status = np.random.choice(PR_STATUS, p=[0.75, 0.10, 0.15])
                merged_at = None
                if pr_status == "Merged":
                    merged_at = created_at + pd.Timedelta(hours=random.randint(1, 96))
                    if merged_at.date() > TODAY:
                        merged_at = pd.Timestamp(TODAY)
                files_changed = int(clip(np.random.gamma(2, 3), 1, 80))
                additions = int(clip(np.random.gamma(2, 40), 1, 3000))
                deletions = int(clip(np.random.gamma(1.5, 25), 0, 2000))
                review_count = random.choice([0, 1, 1, 2, 2, 3]) if random.random() > 0.05 else None
                pr_rows.append(dict(
                    pull_request_id=f"PR-{pid}-{pr_counter:06d}", project_id=pid, employee_id=emp,
                    created_at=created_at.isoformat(sep=" "),
                    merged_at=merged_at.isoformat(sep=" ") if merged_at is not None else None,
                    status=pr_status, files_changed=files_changed, additions=additions,
                    deletions=deletions, review_count=review_count,
                ))

            # --- issues ---
            n_issues = np.random.poisson(clip(team_size * 0.35 * activity_scale, 0.2, 40))
            for _ in range(n_issues):
                issue_counter += 1
                emp = random.choice(dev_emps)
                created_at = rand_datetime_in_month(period)
                issue_status = np.random.choice(["Fechado", "Em Andamento", "Aberto"], p=[0.6, 0.2, 0.2])
                closed_at = None
                if issue_status == "Fechado":
                    closed_at = created_at + pd.Timedelta(days=random.randint(1, 45))
                    if closed_at.date() > TODAY:
                        closed_at = pd.Timestamp(TODAY)
                issue_rows.append(dict(
                    issue_id=f"ISS-{pid}-{issue_counter:06d}", project_id=pid, employee_id=emp,
                    issue_type=np.random.choice(ISSUE_TYPES, p=[0.4, 0.32, 0.18, 0.10]),
                    priority=np.random.choice(ISSUE_PRIORITIES, p=[0.3, 0.4, 0.22, 0.08]),
                    created_at=created_at.isoformat(sep=" "),
                    closed_at=closed_at.isoformat(sep=" ") if closed_at is not None else None,
                    status=issue_status,
                ))

            # --- builds (CI) ---
            n_builds = np.random.poisson(clip(team_size * 1.1 * activity_scale, 0.5, 150))
            for _ in range(n_builds):
                build_counter += 1
                ts = rand_datetime_in_month(period)
                b_status = np.random.choice(BUILD_STATUS, p=[0.85, 0.15])
                tests_executed = int(clip(np.random.normal(400, 200), 10, 3000))
                if b_status == "Falha":
                    tests_failed = int(clip(np.random.gamma(1.5, 4), 1, tests_executed))
                else:
                    tests_failed = 0 if random.random() > 0.05 else int(clip(np.random.gamma(1, 1.5), 1, 5))
                build_rows.append(dict(
                    build_id=f"BLD-{pid}-{build_counter:06d}", project_id=pid, timestamp=ts.isoformat(sep=" "),
                    status=b_status, duration=int(clip(np.random.normal(320, 150), 20, 1800)),
                    tests_executed=tests_executed, tests_failed=tests_failed,
                ))

            # --- deployments ---
            n_deploys = np.random.poisson(clip(team_size * 0.18 * activity_scale, 0.1, 20))
            for _ in range(n_deploys):
                deploy_counter += 1
                ts = rand_datetime_in_month(period)
                env = np.random.choice(DEPLOY_ENVS, p=[0.5, 0.3, 0.2])
                d_status = np.random.choice(DEPLOY_STATUS, p=[0.88, 0.08, 0.04])
                deploy_rows.append(dict(
                    deployment_id=f"DEP-{pid}-{deploy_counter:06d}", project_id=pid,
                    timestamp=ts.isoformat(sep=" "), environment=env, status=d_status,
                ))

pull_requests_df = pd.DataFrame(pr_rows)
issues_df = pd.DataFrame(issue_rows)
builds_df = pd.DataFrame(build_rows)
deployments_df = pd.DataFrame(deploy_rows)
print(f"  pull_requests: {len(pull_requests_df)} linhas")
print(f"  issues: {len(issues_df)} linhas")
print(f"  builds: {len(builds_df)} linhas")
print(f"  deployments: {len(deployments_df)} linhas")

# --------------------------------------------------------------------------
# EXPORTACAO: CSV + SQLite
# --------------------------------------------------------------------------

TABLES = {
    "companies": companies_df,
    "departments": departments_df,
    "employees": employees_df,
    "ai_tools": AI_TOOLS_DF,
    "ai_models": AI_MODELS_DF,
    "ai_usage_logs": usage_logs_df,
    "ai_billing": billing_df,
    "ai_budgets": budgets_df,
    "ai_licenses": licenses_df[[
        "company_id", "employee_id", "ai_tool_id", "license_type",
        "assigned_date", "expiration_date", "license_status",
    ]],
    "training_records": training_df,
    "approved_ai_tools": approved_ai_tools_df,
    "security_events": security_events_df,
    "projects": projects_df,
    "pull_requests": pull_requests_df,
    "issues": issues_df,
    "builds": builds_df,
    "deployments": deployments_df,
}

print("\nExportando CSVs...")
for name, df in TABLES.items():
    path = os.path.join(CSV_DIR, f"{name}.csv")
    df.to_csv(path, index=False)
    print(f"  {name}.csv -> {len(df)} linhas")

print("\nExportando banco SQLite...")
# SQLite usa arquivos de journal/WAL que nao funcionam bem sobre o
# filesystem FUSE montado; monta-se o banco em /tmp e depois copia-se
# o arquivo final (ja fechado) para o destino.
import shutil
import tempfile

tmp_db_path = os.path.join(tempfile.gettempdir(), "ai_usage_synthetic_build.db")
if os.path.exists(tmp_db_path):
    os.remove(tmp_db_path)
conn = sqlite3.connect(tmp_db_path)
for name, df in TABLES.items():
    df.to_sql(name, conn, if_exists="replace", index=False)
conn.commit()
conn.close()

if os.path.exists(DB_PATH):
    os.remove(DB_PATH)
shutil.copyfile(tmp_db_path, DB_PATH)
os.remove(tmp_db_path)
print(f"  banco gerado em: {DB_PATH}")

print("\nConcluido. Seed utilizada:", SEED)

