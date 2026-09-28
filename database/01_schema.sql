-- =====================================================================
-- DAMA Intelligence — Schema transacional (PostgreSQL 16/17)
-- Redesenhado a partir do documento de requisitos (RF01-RF38, RNF01-08,
-- RN01-07) em substituição ao schema anterior, que modelava outro
-- domínio (analytics de adoção de ferramentas de IA entre empresas-
-- cliente) e não correspondia ao produto descrito.
--
-- Convenções:
--   * ids como bigserial/uuid conforme o volume esperado da entidade
--   * timestamps sem fuso (mesma convenção do projeto anterior)
--   * "empresa" é o limite de tenant (RF/RN não descrevem multiempresa
--     explicitamente, mas a Visão do Produto e as personas indicam que
--     o DAMA é vendido a várias empresas-cliente; "equipe" nos RFs =
--     departamento DENTRO de uma empresa, nunca outra empresa)
-- Este script é idempotente: recria o schema "dama" do zero.
-- =====================================================================
DROP SCHEMA IF EXISTS dama CASCADE;
CREATE SCHEMA dama;
SET search_path = dama, public;

-- ---------------------------------------------------------------------
-- E01 — Gestão de Colaboradores (RF01, RF02, RF16, RF17, RF18)
-- ---------------------------------------------------------------------

CREATE TABLE empresa (
    empresa_id    bigserial PRIMARY KEY,
    nome          text        NOT NULL,
    setor         text,
    porte         text        CHECK (porte IN ('Pequena','Media','Grande')),
    criado_em     timestamp   NOT NULL DEFAULT now()
);

CREATE TABLE departamento (
    departamento_id  bigserial PRIMARY KEY,
    empresa_id       bigint      NOT NULL REFERENCES empresa,
    nome             text        NOT NULL,
    UNIQUE (empresa_id, nome)
);
COMMENT ON TABLE departamento IS
  'Corresponde à "equipe" citada em RF14/RF36 (comparação sempre DENTRO de uma empresa).';

CREATE TABLE colaborador (
    colaborador_id   bigserial PRIMARY KEY,
    empresa_id       bigint      NOT NULL REFERENCES empresa,
    departamento_id  bigint      NOT NULL REFERENCES departamento,
    nome             text        NOT NULL,
    email            text        NOT NULL,
    cargo            text,
    senioridade      text        CHECK (senioridade IN ('Junior','Pleno','Senior','Especialista','Gerente')),
    data_admissao    date        NOT NULL,
    ativo            boolean     NOT NULL DEFAULT true,
    criado_em        timestamp   NOT NULL DEFAULT now(),
    atualizado_em    timestamp   NOT NULL DEFAULT now(),
    UNIQUE (empresa_id, email)
);
COMMENT ON COLUMN colaborador.ativo IS
  'Desligamento é lógico (RF02 fala em "editar", não há RF de exclusão física).';

-- Um departamento pertence à mesma empresa do colaborador: garantido na
-- camada de aplicação (ColaboradorService) além do FK, pois checar isso
-- só em CHECK exigiria uma consulta cruzada que o Postgres não permite
-- em CHECK simples.

-- ---------------------------------------------------------------------
-- Perfis e permissões (RF18, RNF02, RN01, RN07)
-- ---------------------------------------------------------------------

CREATE TABLE perfil (
    perfil_id    bigserial PRIMARY KEY,
    nome         text UNIQUE NOT NULL,     -- ex.: Administrador, Gestor, Colaborador
    descricao    text
);

CREATE TABLE permissao (
    permissao_id  bigserial PRIMARY KEY,
    codigo        text UNIQUE NOT NULL,    -- ex.: COLABORADOR_ESCREVER, AI_CREDITS_GERENCIAR
    descricao     text
);

CREATE TABLE perfil_permissao (
    perfil_id     bigint NOT NULL REFERENCES perfil ON DELETE CASCADE,
    permissao_id  bigint NOT NULL REFERENCES permissao ON DELETE CASCADE,
    PRIMARY KEY (perfil_id, permissao_id)
);

CREATE TABLE colaborador_perfil (
    colaborador_id  bigint NOT NULL REFERENCES colaborador ON DELETE CASCADE,
    perfil_id       bigint NOT NULL REFERENCES perfil,
    atribuido_em    timestamp NOT NULL DEFAULT now(),
    PRIMARY KEY (colaborador_id, perfil_id)
);

-- ---------------------------------------------------------------------
-- E02 — Gestão de Desempenho (RF03, RF04, RF05, RF06, RF19, RF20,
-- RF29, RF30, RF31, RF32-RF35, RNF04, RNF08)
-- ---------------------------------------------------------------------

CREATE TABLE indicador (
    indicador_id  bigserial PRIMARY KEY,
    empresa_id    bigint NOT NULL REFERENCES empresa,
    nome          text   NOT NULL,
    descricao     text,
    unidade       text,                 -- ex.: "%", "pontos", "unidades/mês" (RNF08)
    maior_melhor  boolean NOT NULL DEFAULT true,
    ativo         boolean NOT NULL DEFAULT true,
    UNIQUE (empresa_id, nome)
);
COMMENT ON COLUMN indicador.maior_melhor IS
  'false para indicadores em que menor é melhor (ex.: retrabalho). Usado na conclusão de metas e nas análises de queda.';

CREATE TABLE indicador_valor (
    indicador_valor_id  bigserial PRIMARY KEY,
    indicador_id        bigint  NOT NULL REFERENCES indicador,
    colaborador_id       bigint  REFERENCES colaborador,
    departamento_id      bigint  REFERENCES departamento,
    periodo_referencia   date    NOT NULL,   -- normalizado para o dia 1 do mês
    valor                numeric NOT NULL,
    registrado_por        bigint  REFERENCES colaborador,
    registrado_em         timestamp NOT NULL DEFAULT now(),
    CHECK (colaborador_id IS NOT NULL OR departamento_id IS NOT NULL)
);
COMMENT ON TABLE indicador_valor IS
  'Série histórica: um indicador tem N valores ao longo do tempo (RF06 evolução, RNF04 consistência com o dashboard).';

CREATE TABLE atividade_produtiva (
    atividade_id  bigserial PRIMARY KEY,
    empresa_id    bigint NOT NULL REFERENCES empresa,
    nome          text   NOT NULL,
    descricao     text,
    ativa         boolean NOT NULL DEFAULT true
);
COMMENT ON TABLE atividade_produtiva IS 'RF19 — catálogo de atividades que podem ser registradas e, quando elegíveis, gerar AI Credits ou pontuação.';

CREATE TABLE atividade_registro (
    registro_id     bigserial PRIMARY KEY,
    atividade_id    bigint  NOT NULL REFERENCES atividade_produtiva,
    colaborador_id  bigint  NOT NULL REFERENCES colaborador,
    quantidade      numeric NOT NULL DEFAULT 1,
    registrado_em   timestamp NOT NULL DEFAULT now()
);
COMMENT ON TABLE atividade_registro IS 'RF20 — atividade efetivamente realizada por um colaborador.';

CREATE TABLE meta (
    meta_id          bigserial PRIMARY KEY,
    empresa_id       bigint  NOT NULL REFERENCES empresa,
    indicador_id     bigint  REFERENCES indicador,
    colaborador_id   bigint  REFERENCES colaborador,
    departamento_id  bigint  REFERENCES departamento,
    descricao        text    NOT NULL,
    valor_alvo       numeric NOT NULL,
    data_inicio      date    NOT NULL,
    data_fim         date    NOT NULL,
    status           text    NOT NULL DEFAULT 'Em andamento'
                     CHECK (status IN ('Em andamento','Concluida','Atrasada','Cancelada')),
    CHECK (colaborador_id IS NOT NULL OR departamento_id IS NOT NULL),  -- RN05
    CHECK (data_fim >= data_inicio)
);

CREATE TABLE meta_progresso (
    meta_progresso_id  bigserial PRIMARY KEY,
    meta_id            bigint  NOT NULL REFERENCES meta ON DELETE CASCADE,
    valor_atual        numeric NOT NULL,
    registrado_em      timestamp NOT NULL DEFAULT now()
);
COMMENT ON TABLE meta_progresso IS 'RF30 — histórico de progresso de uma meta ao longo do tempo.';

-- ---------------------------------------------------------------------
-- E03 — Gamificação (RF07, RF08, RF09, RF10, RF27, RF28, RF31, RN02, RN03)
-- ---------------------------------------------------------------------

CREATE TABLE regra_pontuacao (
    regra_id      bigserial PRIMARY KEY,
    empresa_id    bigint  NOT NULL REFERENCES empresa,
    atividade_id  bigint  REFERENCES atividade_produtiva,
    nome          text    NOT NULL,
    pontos        numeric NOT NULL,
    ativa         boolean NOT NULL DEFAULT true
);
COMMENT ON TABLE regra_pontuacao IS 'RN02 — critério definido de pontos por atividade/evento pontuável.';

CREATE TABLE pontuacao (
    pontuacao_id    bigserial PRIMARY KEY,
    colaborador_id  bigint  NOT NULL REFERENCES colaborador,
    regra_id        bigint  REFERENCES regra_pontuacao,
    pontos          numeric NOT NULL,
    descricao       text,
    registrado_em   timestamp NOT NULL DEFAULT now()
);
COMMENT ON TABLE pontuacao IS 'RF07/RF27 — lançamento de pontos; RF11 histórico é a consulta desta tabela por colaborador.';

CREATE TABLE desafio (
    desafio_id        bigserial PRIMARY KEY,
    empresa_id        bigint  NOT NULL REFERENCES empresa,
    nome              text    NOT NULL,
    descricao         text,
    data_inicio       date    NOT NULL,
    data_fim          date    NOT NULL,
    pontos_recompensa numeric NOT NULL DEFAULT 0,
    status            text    NOT NULL DEFAULT 'Planejado'
                       CHECK (status IN ('Planejado','Em andamento','Encerrado','Cancelado')),
    CHECK (data_fim >= data_inicio)
);

CREATE TABLE desafio_participante (
    desafio_id      bigint NOT NULL REFERENCES desafio ON DELETE CASCADE,
    colaborador_id  bigint NOT NULL REFERENCES colaborador,
    status          text   NOT NULL DEFAULT 'Inscrito'
                     CHECK (status IN ('Inscrito','Concluido','Desistente')),
    pontos_obtidos  numeric NOT NULL DEFAULT 0,
    PRIMARY KEY (desafio_id, colaborador_id)
);

CREATE TABLE reconhecimento (
    reconhecimento_id  bigserial PRIMARY KEY,
    empresa_id         bigint NOT NULL REFERENCES empresa,
    colaborador_id     bigint NOT NULL REFERENCES colaborador,   -- quem recebe
    concedido_por_id   bigint REFERENCES colaborador,            -- quem concede (gestor/par)
    tipo               text   NOT NULL,
    descricao          text   NOT NULL,
    registrado_em      timestamp NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- Conquistas e níveis de evolução (RF31)
-- ---------------------------------------------------------------------

CREATE TABLE nivel_evolucao (
    nivel_id        bigserial PRIMARY KEY,
    nome            text    NOT NULL UNIQUE,
    pontos_minimos  numeric NOT NULL CHECK (pontos_minimos >= 0),
    ordem           int     NOT NULL UNIQUE,
    descricao       text
);
COMMENT ON TABLE nivel_evolucao IS
  'Nível de evolução do colaborador: o maior nível cujo pontos_minimos já foi atingido pela soma de pontuacao.';

CREATE TABLE conquista (
    conquista_id  bigserial PRIMARY KEY,
    empresa_id    bigint  NOT NULL REFERENCES empresa,
    nome          text    NOT NULL,
    descricao     text,
    criterio      text    NOT NULL CHECK (criterio IN (
                      'PONTOS_TOTAIS', 'METAS_CONCLUIDAS', 'DESAFIOS_CONCLUIDOS',
                      'RECONHECIMENTOS_RECEBIDOS', 'ATIVIDADES_REGISTRADAS',
                      'AI_CREDITS_ACUMULADOS', 'MANUAL')),
    valor_minimo  numeric CHECK (valor_minimo > 0),
    ativa         boolean NOT NULL DEFAULT true,
    UNIQUE (empresa_id, nome),
    CHECK ((criterio = 'MANUAL') = (valor_minimo IS NULL))
);
COMMENT ON TABLE conquista IS
  'Conquista definida pela empresa. Critérios automáticos são avaliados a cada movimentação do colaborador; MANUAL é concedida por um gestor.';

CREATE TABLE colaborador_conquista (
    colaborador_id    bigint    NOT NULL REFERENCES colaborador,
    conquista_id      bigint    NOT NULL REFERENCES conquista,
    obtida_em         timestamp NOT NULL DEFAULT now(),
    concedida_por_id  bigint    REFERENCES colaborador,   -- nulo = concessão automática
    PRIMARY KEY (colaborador_id, conquista_id)
);

-- ---------------------------------------------------------------------
-- E04 — AI Credits (RF12, RF21-RF26, RN04)
-- ---------------------------------------------------------------------

CREATE TABLE ai_credit_regra (
    regra_id                bigserial PRIMARY KEY,
    empresa_id              bigint  NOT NULL REFERENCES empresa,
    atividade_id            bigint  REFERENCES atividade_produtiva,
    nome                    text    NOT NULL,
    criterio_elegibilidade  text    NOT NULL,   -- descrição textual da regra (RN04)
    creditos_concedidos     numeric NOT NULL,
    ativa                   boolean NOT NULL DEFAULT true
);
COMMENT ON TABLE ai_credit_regra IS 'RF21 — elegibilidade de uma atividade para gerar AI Credits.';

CREATE TABLE ai_credit_transacao (
    transacao_id    bigserial PRIMARY KEY,
    colaborador_id  bigint  NOT NULL REFERENCES colaborador,
    regra_id        bigint  REFERENCES ai_credit_regra,
    registro_id     bigint  REFERENCES atividade_registro,  -- atividade de origem, quando houver
    tipo            text    NOT NULL CHECK (tipo IN ('CREDITO','DEBITO')),
    quantidade      numeric NOT NULL CHECK (quantidade > 0),
    motivo          text    NOT NULL,
    registrado_em   timestamp NOT NULL DEFAULT now()
);
COMMENT ON TABLE ai_credit_transacao IS 'RF22 atribuição, RF24 histórico; RF23 (saldo) é SUM(CREDITO) - SUM(DEBITO), nunca armazenado.';

CREATE TABLE ai_nivel_autonomia (
    nivel_id            bigserial PRIMARY KEY,
    nome                text    NOT NULL,      -- ex.: Basico, Ampliado, Total
    creditos_minimos    numeric NOT NULL,
    ordem               int     NOT NULL,
    descricao           text
);
COMMENT ON TABLE ai_nivel_autonomia IS 'RF25/RF26 — nível de acesso à IA desbloqueado conforme o saldo de créditos.';

-- ---------------------------------------------------------------------
-- E05 — Analytics (RF13, RF14, RF15, RF36, RF37) — deriva de indicador,
-- meta, pontuacao e ai_credit_transacao acima; não tem tabelas próprias
-- além de auditoria abaixo. Ver database/02_views.sql.
-- ---------------------------------------------------------------------

-- ---------------------------------------------------------------------
-- Auditoria (RF11 histórico geral, RF38, RN01/RN07 rastreio de acesso)
-- ---------------------------------------------------------------------

CREATE TABLE auditoria_evento (
    evento_id       bigserial PRIMARY KEY,
    empresa_id      bigint  NOT NULL REFERENCES empresa,
    colaborador_id  bigint  REFERENCES colaborador,   -- quem realizou a ação (nulo = sistema)
    entidade        text    NOT NULL,                 -- ex.: 'colaborador', 'meta', 'ai_credit_transacao'
    entidade_id     text    NOT NULL,
    acao            text    NOT NULL,                 -- ex.: 'CRIACAO','ATUALIZACAO','EXCLUSAO'
    detalhes         jsonb,
    registrado_em   timestamp NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- Índices de apoio às consultas de dashboard/ranking/analytics
-- ---------------------------------------------------------------------

CREATE INDEX ix_colaborador_empresa       ON colaborador (empresa_id);
CREATE INDEX ix_colaborador_departamento  ON colaborador (departamento_id);
CREATE INDEX ix_indicador_valor_periodo   ON indicador_valor (indicador_id, periodo_referencia);
CREATE INDEX ix_meta_departamento         ON meta (departamento_id);
CREATE INDEX ix_pontuacao_colaborador     ON pontuacao (colaborador_id, registrado_em);
CREATE INDEX ix_ai_credit_colaborador     ON ai_credit_transacao (colaborador_id, registrado_em);
CREATE INDEX ix_auditoria_empresa         ON auditoria_evento (empresa_id, registrado_em);
CREATE INDEX ix_atividade_registro_colab  ON atividade_registro (colaborador_id, registrado_em);
CREATE INDEX ix_reconhecimento_colab      ON reconhecimento (colaborador_id);
