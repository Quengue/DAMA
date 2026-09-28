-- =====================================================================
-- Views de apoio ao módulo analytics (RF13, RF14, RF15, RF36, RF37).
-- Diferença essencial em relação à versão anterior: tudo aqui é
-- calculado DENTRO de uma empresa (por departamento/colaborador), nunca
-- comparando uma empresa-cliente com outra.
-- =====================================================================
SET search_path = dama, public;

-- RF33/RF36 — indicadores da equipe: valor mais recente por indicador/departamento
-- (DROP antes do CREATE porque a lista de colunas mudou; mantém o script reexecutável)
DROP VIEW IF EXISTS vw_indicador_departamento_atual;
CREATE VIEW vw_indicador_departamento_atual AS
SELECT iv.departamento_id,
       d.nome AS departamento_nome,
       i.empresa_id,
       i.indicador_id,
       i.nome AS indicador_nome,
       i.unidade,
       iv.periodo_referencia,
       iv.valor
FROM indicador_valor iv
JOIN indicador i ON i.indicador_id = iv.indicador_id
JOIN departamento d ON d.departamento_id = iv.departamento_id
JOIN (
    SELECT departamento_id, indicador_id, MAX(periodo_referencia) AS ultimo_periodo
    FROM indicador_valor
    WHERE departamento_id IS NOT NULL
    GROUP BY departamento_id, indicador_id
) ult ON ult.departamento_id = iv.departamento_id
     AND ult.indicador_id = iv.indicador_id
     AND ult.ultimo_periodo = iv.periodo_referencia
WHERE iv.departamento_id IS NOT NULL;

-- RF35 — evolução de um indicador ao longo do tempo, por colaborador ou equipe
CREATE OR REPLACE VIEW vw_indicador_evolucao AS
SELECT iv.indicador_id,
       i.empresa_id,
       iv.colaborador_id,
       iv.departamento_id,
       iv.periodo_referencia,
       iv.valor
FROM indicador_valor iv
JOIN indicador i ON i.indicador_id = iv.indicador_id
ORDER BY iv.periodo_referencia;

-- RF08/RF28 — ranking de colaboradores por pontuação, dentro da empresa
CREATE OR REPLACE VIEW vw_ranking_colaborador AS
SELECT c.empresa_id,
       c.colaborador_id,
       c.nome,
       c.departamento_id,
       COALESCE(SUM(p.pontos), 0) AS pontos_totais,
       RANK() OVER (PARTITION BY c.empresa_id ORDER BY COALESCE(SUM(p.pontos), 0) DESC) AS posicao
FROM colaborador c
LEFT JOIN pontuacao p ON p.colaborador_id = c.colaborador_id
WHERE c.ativo
GROUP BY c.empresa_id, c.colaborador_id, c.nome, c.departamento_id;

-- RF14/RF36 — comparação entre equipes (departamentos) de uma mesma empresa
CREATE OR REPLACE VIEW vw_ranking_departamento AS
SELECT d.empresa_id,
       d.departamento_id,
       d.nome AS departamento_nome,
       COUNT(DISTINCT c.colaborador_id) FILTER (WHERE c.ativo) AS colaboradores_ativos,
       COALESCE(SUM(p.pontos), 0) AS pontos_totais,
       COALESCE(AVG(p.pontos), 0) AS pontos_media_por_lancamento,
       RANK() OVER (PARTITION BY d.empresa_id ORDER BY COALESCE(SUM(p.pontos), 0) DESC) AS posicao
FROM departamento d
LEFT JOIN colaborador c ON c.departamento_id = d.departamento_id
LEFT JOIN pontuacao p ON p.colaborador_id = c.colaborador_id
GROUP BY d.empresa_id, d.departamento_id, d.nome;

-- RF13 — visão consolidada de desempenho de uma empresa
CREATE OR REPLACE VIEW vw_visao_consolidada_empresa AS
SELECT e.empresa_id,
       COUNT(DISTINCT c.colaborador_id) FILTER (WHERE c.ativo)      AS colaboradores_ativos,
       COUNT(DISTINCT d.departamento_id)                             AS total_departamentos,
       COUNT(DISTINCT m.meta_id) FILTER (WHERE m.status = 'Em andamento') AS metas_em_andamento,
       COUNT(DISTINCT m.meta_id) FILTER (WHERE m.status = 'Concluida')     AS metas_concluidas,
       COALESCE(SUM(p.pontos), 0)                                    AS pontos_totais_periodo
FROM empresa e
LEFT JOIN colaborador c ON c.empresa_id = e.empresa_id
LEFT JOIN departamento d ON d.empresa_id = e.empresa_id
LEFT JOIN meta m ON m.empresa_id = e.empresa_id
LEFT JOIN pontuacao p ON p.colaborador_id = c.colaborador_id
GROUP BY e.empresa_id;

-- RF15 — saldo de AI Credits por colaborador (nunca armazenado, sempre derivado)
CREATE OR REPLACE VIEW vw_ai_credits_saldo AS
SELECT colaborador_id,
       SUM(CASE WHEN tipo = 'CREDITO' THEN quantidade ELSE -quantidade END) AS saldo_atual
FROM ai_credit_transacao
GROUP BY colaborador_id;
