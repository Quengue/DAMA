-- =====================================================================
-- Dados de demonstração da "Empresa Demonstração" (empresa_id = 1).
-- Opcional: carregado pelo docker-compose da raiz. Datas são relativas a
-- current_date, então a demo continua coerente em qualquer dia.
-- Personas (documento de visão): Mariana (gestora), Lucas (colaborador),
-- Ricardo (diretor).
-- =====================================================================
SET search_path = dama, public;

CREATE FUNCTION pg_temp.cid(email text) RETURNS bigint LANGUAGE sql AS
    $$ SELECT colaborador_id FROM dama.colaborador WHERE colaborador.email = cid.email $$;
CREATE FUNCTION pg_temp.did(nome text) RETURNS bigint LANGUAGE sql AS
    $$ SELECT departamento_id FROM dama.departamento WHERE empresa_id = 1 AND departamento.nome = did.nome $$;
CREATE FUNCTION pg_temp.iid(nome text) RETURNS bigint LANGUAGE sql AS
    $$ SELECT indicador_id FROM dama.indicador WHERE empresa_id = 1 AND indicador.nome = iid.nome $$;
CREATE FUNCTION pg_temp.rid(nome text) RETURNS bigint LANGUAGE sql AS
    $$ SELECT regra_id FROM dama.regra_pontuacao WHERE empresa_id = 1 AND regra_pontuacao.nome = rid.nome $$;
CREATE FUNCTION pg_temp.aid(nome text) RETURNS bigint LANGUAGE sql AS
    $$ SELECT atividade_id FROM dama.atividade_produtiva WHERE empresa_id = 1 AND atividade_produtiva.nome = aid.nome $$;
CREATE FUNCTION pg_temp.mes(meses_atras int) RETURNS date LANGUAGE sql AS
    $$ SELECT (date_trunc('month', current_date) - make_interval(months => meses_atras))::date $$;

-- ---------------------------------------------------------------------
-- Estrutura e pessoas
-- ---------------------------------------------------------------------
INSERT INTO departamento (empresa_id, nome) VALUES (1, 'Tecnologia'), (1, 'Comercial');

INSERT INTO colaborador (empresa_id, departamento_id, nome, email, cargo, senioridade, data_admissao, ativo)
SELECT 1, pg_temp.did(dep), nome, email, cargo, senioridade, admissao, ativo
FROM (VALUES
    ('Recursos Humanos', 'Mariana Souza',    'mariana.souza@demo.dama',    'Gerente de Pessoas',       'Gerente',      DATE '2021-03-01', true),
    ('Operações',        'Ricardo Nogueira', 'ricardo.nogueira@demo.dama', 'Diretor de Operações',     'Gerente',      DATE '2019-08-12', true),
    ('Tecnologia',       'Lucas Pereira',    'lucas.pereira@demo.dama',    'Analista de Dados',        'Pleno',        DATE '2023-02-06', true),
    ('Tecnologia',       'Beatriz Santos',   'beatriz.santos@demo.dama',   'Engenheira de Software',   'Senior',       DATE '2021-11-22', true),
    ('Tecnologia',       'Felipe Costa',     'felipe.costa@demo.dama',     'Desenvolvedor',            'Junior',       DATE '2025-01-13', true),
    ('Tecnologia',       'Juliana Rocha',    'juliana.rocha@demo.dama',    'Arquiteta de Soluções',    'Especialista', DATE '2020-05-04', true),
    ('Comercial',        'Camila Ferreira',  'camila.ferreira@demo.dama',  'Executiva de Contas',      'Pleno',        DATE '2022-09-19', true),
    ('Comercial',        'Diego Martins',    'diego.martins@demo.dama',    'Pré-vendas',               'Junior',       DATE '2024-10-01', true),
    ('Comercial',        'Paula Mendes',     'paula.mendes@demo.dama',     'Gerente de Contas',        'Senior',       DATE '2020-02-10', true),
    ('Comercial',        'Henrique Alves',   'henrique.alves@demo.dama',   'Executivo de Contas',      'Pleno',        DATE '2022-04-04', false),
    ('Operações',        'Rafael Oliveira',  'rafael.oliveira@demo.dama',  'Analista de Processos',    'Pleno',        DATE '2022-07-18', true),
    ('Operações',        'Tatiane Lima',     'tatiane.lima@demo.dama',     'Assistente Operacional',   'Junior',       DATE '2024-03-11', true),
    ('Recursos Humanos', 'Gabriel Souza',    'gabriel.souza@demo.dama',    'Analista de RH',           'Pleno',        DATE '2023-06-26', true)
) AS p(dep, nome, email, cargo, senioridade, admissao, ativo);

INSERT INTO colaborador_perfil (colaborador_id, perfil_id)
SELECT c.colaborador_id, p.perfil_id
FROM colaborador c
JOIN perfil p ON p.nome = CASE WHEN c.email IN ('mariana.souza@demo.dama', 'ricardo.nogueira@demo.dama')
                               THEN 'Gestor' ELSE 'Colaborador' END
WHERE c.email LIKE '%@demo.dama';

-- ---------------------------------------------------------------------
-- Indicadores e 6 meses de histórico
-- ---------------------------------------------------------------------
INSERT INTO indicador (empresa_id, nome, descricao, unidade, maior_melhor) VALUES
    (1, 'Entregas no prazo',          'Percentual de entregas concluídas dentro do prazo', '%',              true),
    (1, 'Tarefas concluídas com IA',  'Tarefas em que a IA foi usada e o resultado aprovado', 'tarefas/mês',  true),
    (1, 'Eficiência de tokens',       'Entregas aprovadas a cada mil tokens consumidos',    'entregas/1k tokens', true),
    (1, 'Retrabalho',                 'Percentual de entregas devolvidas para correção',    '%',              false),
    (1, 'Satisfação do cliente',      'NPS apurado pela equipe comercial',                  'NPS',            true);

-- valor = base + tendência por mês + ruído de até ±4%; "ultimo" multiplica só o mês atual
-- (usado para simular quedas que as análises de gestão devem apontar)
INSERT INTO indicador_valor (indicador_id, colaborador_id, periodo_referencia, valor, registrado_por)
SELECT pg_temp.iid(v.indicador), pg_temp.cid(v.email), pg_temp.mes(m),
       round((v.base + v.tendencia * (5 - m)
              + v.base * 0.02 * (((pg_temp.cid(v.email) * 7 + m * 3) % 5) - 2))
             * CASE WHEN m = 0 THEN v.ultimo ELSE 1 END, 1),
       pg_temp.cid('mariana.souza@demo.dama')
FROM (VALUES
    ('lucas.pereira@demo.dama',   'Entregas no prazo',          78, 2.0, 1.0),
    ('lucas.pereira@demo.dama',   'Tarefas concluídas com IA',  12, 1.5, 1.0),
    ('lucas.pereira@demo.dama',   'Eficiência de tokens',       3.2, 0.2, 1.0),
    ('beatriz.santos@demo.dama',  'Entregas no prazo',          88, 1.0, 1.0),
    ('beatriz.santos@demo.dama',  'Tarefas concluídas com IA',  20, 1.2, 1.0),
    ('beatriz.santos@demo.dama',  'Retrabalho',                 9, -0.5, 1.0),
    ('felipe.costa@demo.dama',    'Entregas no prazo',          70, 1.5, 1.0),
    ('felipe.costa@demo.dama',    'Retrabalho',                 12, -0.2, 1.4),
    ('juliana.rocha@demo.dama',   'Entregas no prazo',          92, 0.5, 1.0),
    ('juliana.rocha@demo.dama',   'Eficiência de tokens',       4.5, 0.3, 1.0),
    ('camila.ferreira@demo.dama', 'Entregas no prazo',          81, 0.5, 0.84),
    ('diego.martins@demo.dama',   'Entregas no prazo',          76, 0.0, 0.68),
    ('paula.mendes@demo.dama',    'Entregas no prazo',          85, 0.6, 1.0),
    ('rafael.oliveira@demo.dama', 'Entregas no prazo',          83, 1.2, 1.0),
    ('rafael.oliveira@demo.dama', 'Tarefas concluídas com IA',  9, 1.8, 1.0),
    ('tatiane.lima@demo.dama',    'Entregas no prazo',          74, 0.0, 1.0),
    ('gabriel.souza@demo.dama',   'Entregas no prazo',          86, 0.4, 1.0)
) AS v(email, indicador, base, tendencia, ultimo)
CROSS JOIN generate_series(0, 5) AS m;

INSERT INTO indicador_valor (indicador_id, departamento_id, periodo_referencia, valor, registrado_por)
SELECT pg_temp.iid(v.indicador), pg_temp.did(v.dep), pg_temp.mes(m),
       round((v.base + v.tendencia * (5 - m)) * CASE WHEN m = 0 THEN v.ultimo ELSE 1 END, 1),
       pg_temp.cid('ricardo.nogueira@demo.dama')
FROM (VALUES
    ('Tecnologia',       'Entregas no prazo',     82, 1.2, 1.0),
    ('Tecnologia',       'Retrabalho',            11, -0.6, 1.0),
    ('Comercial',        'Entregas no prazo',     80, 0.3, 1.0),
    ('Comercial',        'Satisfação do cliente', 72, 0.8, 0.86),
    ('Operações',        'Entregas no prazo',     79, 0.9, 1.0),
    ('Recursos Humanos', 'Entregas no prazo',     85, 0.3, 1.0)
) AS v(dep, indicador, base, tendencia, ultimo)
CROSS JOIN generate_series(0, 5) AS m;

-- ---------------------------------------------------------------------
-- Metas e progresso
-- ---------------------------------------------------------------------
INSERT INTO meta (empresa_id, indicador_id, colaborador_id, departamento_id, descricao, valor_alvo, data_inicio, data_fim, status)
SELECT 1, pg_temp.iid(indicador), pg_temp.cid(email), pg_temp.did(dep), descricao, alvo,
       current_date + inicio, current_date + fim, status
FROM (VALUES
    ('Entregas no prazo',          'lucas.pereira@demo.dama',   NULL,         'Chegar a 90% de entregas no prazo',          90,  -90,  60, 'Em andamento'),
    ('Tarefas concluídas com IA',  'beatriz.santos@demo.dama',  NULL,         'Concluir 25 tarefas com apoio de IA no mês', 25,  -60, -10, 'Concluida'),
    ('Retrabalho',                 'felipe.costa@demo.dama',    NULL,         'Reduzir o retrabalho para 8%',               8,   -45,  75, 'Em andamento'),
    (NULL,                         'camila.ferreira@demo.dama', NULL,         'Fechar 12 contratos no trimestre',           12,  -80,  10, 'Em andamento'),
    (NULL,                         'diego.martins@demo.dama',   NULL,         'Agendar 40 reuniões qualificadas',           40,  -60,  -5, 'Em andamento'),
    (NULL,                         'juliana.rocha@demo.dama',   NULL,         'Publicar 3 guias internos de uso de IA',     3,   -30,  60, 'Em andamento'),
    (NULL,                         'rafael.oliveira@demo.dama', NULL,         'Mapear 10 processos para automação com IA',  10,  -120, -20, 'Concluida'),
    ('Satisfação do cliente',      NULL,                        'Comercial',  'NPS da equipe acima de 75',                  75,  -60,  30, 'Em andamento'),
    (NULL,                         NULL,                        'Tecnologia', 'IA adotada em 100% dos squads',              100, -150, -30, 'Concluida')
) AS v(indicador, email, dep, descricao, alvo, inicio, fim, status);

INSERT INTO meta_progresso (meta_id, valor_atual, registrado_em)
SELECT m.meta_id, p.valor, now() - make_interval(days => p.dias_atras)
FROM (VALUES
    ('Chegar a 90% de entregas no prazo',          82, 60), ('Chegar a 90% de entregas no prazo',          86, 30),
    ('Chegar a 90% de entregas no prazo',          88, 3),
    ('Concluir 25 tarefas com apoio de IA no mês', 18, 30), ('Concluir 25 tarefas com apoio de IA no mês', 26, 12),
    ('Reduzir o retrabalho para 8%',               14, 30), ('Reduzir o retrabalho para 8%',               12, 2),
    ('Fechar 12 contratos no trimestre',           3, 20),
    ('Agendar 40 reuniões qualificadas',           22, 10),
    ('Publicar 3 guias internos de uso de IA',     1, 15),
    ('Mapear 10 processos para automação com IA',  6, 60), ('Mapear 10 processos para automação com IA',  10, 22),
    ('NPS da equipe acima de 75',                  71, 25),
    ('IA adotada em 100% dos squads',              60, 90), ('IA adotada em 100% dos squads',              100, 31)
) AS p(descricao, valor, dias_atras)
JOIN meta m ON m.empresa_id = 1 AND m.descricao = p.descricao;

-- ---------------------------------------------------------------------
-- Pontuação
-- ---------------------------------------------------------------------
INSERT INTO regra_pontuacao (empresa_id, nome, pontos) VALUES
    (1, 'Entrega de projeto',              50),
    (1, 'Meta concluída',                  30),
    (1, 'Boa prática com IA documentada',  20),
    (1, 'Apoio a colega',                  10);

INSERT INTO pontuacao (colaborador_id, regra_id, pontos, descricao, registrado_em)
SELECT pg_temp.cid(email), pg_temp.rid(regra), r.pontos, regra, now() - make_interval(days => dias_atras)
FROM (VALUES
    ('juliana.rocha@demo.dama',   'Entrega de projeto', 80), ('juliana.rocha@demo.dama',   'Entrega de projeto', 45),
    ('juliana.rocha@demo.dama',   'Entrega de projeto', 12), ('juliana.rocha@demo.dama',   'Boa prática com IA documentada', 30),
    ('juliana.rocha@demo.dama',   'Boa prática com IA documentada', 8), ('juliana.rocha@demo.dama', 'Apoio a colega', 5),
    ('beatriz.santos@demo.dama',  'Entrega de projeto', 70), ('beatriz.santos@demo.dama',  'Entrega de projeto', 20),
    ('beatriz.santos@demo.dama',  'Meta concluída', 12), ('beatriz.santos@demo.dama',  'Boa prática com IA documentada', 40),
    ('beatriz.santos@demo.dama',  'Apoio a colega', 4),
    ('lucas.pereira@demo.dama',   'Entrega de projeto', 50), ('lucas.pereira@demo.dama',   'Boa prática com IA documentada', 33),
    ('lucas.pereira@demo.dama',   'Boa prática com IA documentada', 9), ('lucas.pereira@demo.dama', 'Apoio a colega', 2),
    ('felipe.costa@demo.dama',    'Boa prática com IA documentada', 25), ('felipe.costa@demo.dama', 'Apoio a colega', 6),
    ('rafael.oliveira@demo.dama', 'Entrega de projeto', 40), ('rafael.oliveira@demo.dama', 'Meta concluída', 20),
    ('rafael.oliveira@demo.dama', 'Boa prática com IA documentada', 15),
    ('mariana.souza@demo.dama',   'Entrega de projeto', 55), ('mariana.souza@demo.dama',   'Meta concluída', 35),
    ('mariana.souza@demo.dama',   'Boa prática com IA documentada', 7),
    ('gabriel.souza@demo.dama',   'Boa prática com IA documentada', 18), ('gabriel.souza@demo.dama', 'Apoio a colega', 3),
    ('ricardo.nogueira@demo.dama', 'Apoio a colega', 12),
    ('camila.ferreira@demo.dama', 'Apoio a colega', 14),
    ('diego.martins@demo.dama',   'Apoio a colega', 9),
    ('paula.mendes@demo.dama',    'Boa prática com IA documentada', 26),
    ('tatiane.lima@demo.dama',    'Apoio a colega', 70)
) AS v(email, regra, dias_atras)
JOIN regra_pontuacao r ON r.regra_id = pg_temp.rid(v.regra);

-- ---------------------------------------------------------------------
-- Desafios
-- ---------------------------------------------------------------------
INSERT INTO desafio (empresa_id, nome, descricao, data_inicio, data_fim, pontos_recompensa, status) VALUES
    (1, 'Semana da IA produtiva', 'Registrar ao menos três usos de IA com resultado aprovado',
        current_date - 10, current_date + 20, 40, 'Em andamento'),
    (1, 'Hackathon de automação', 'Automatizar um processo interno com IA em 48 horas',
        current_date - 90, current_date - 60, 60, 'Encerrado'),
    (1, 'Maratona de guias de prompts', 'Publicar guias de prompts revisados por pares',
        current_date + 15, current_date + 45, 30, 'Planejado');

INSERT INTO desafio_participante (desafio_id, colaborador_id, status, pontos_obtidos)
SELECT d.desafio_id, pg_temp.cid(v.email), v.status, CASE WHEN v.status = 'Concluido' THEN d.pontos_recompensa ELSE 0 END
FROM (VALUES
    ('Semana da IA produtiva', 'lucas.pereira@demo.dama',   'Inscrito'),
    ('Semana da IA produtiva', 'beatriz.santos@demo.dama',  'Concluido'),
    ('Semana da IA produtiva', 'juliana.rocha@demo.dama',   'Inscrito'),
    ('Semana da IA produtiva', 'camila.ferreira@demo.dama', 'Inscrito'),
    ('Hackathon de automação', 'juliana.rocha@demo.dama',   'Concluido'),
    ('Hackathon de automação', 'rafael.oliveira@demo.dama', 'Concluido'),
    ('Hackathon de automação', 'felipe.costa@demo.dama',    'Desistente')
) AS v(desafio, email, status)
JOIN desafio d ON d.empresa_id = 1 AND d.nome = v.desafio;

-- recompensa dos desafios concluídos entra na pontuação, como faz a API
INSERT INTO pontuacao (colaborador_id, pontos, descricao, registrado_em)
SELECT dp.colaborador_id, dp.pontos_obtidos, 'Desafio concluído: ' || d.nome,
       LEAST(now(), d.data_fim::timestamp)
FROM desafio_participante dp JOIN desafio d ON d.desafio_id = dp.desafio_id
WHERE d.empresa_id = 1 AND dp.status = 'Concluido';

-- ---------------------------------------------------------------------
-- Reconhecimentos
-- ---------------------------------------------------------------------
INSERT INTO reconhecimento (empresa_id, colaborador_id, concedido_por_id, tipo, descricao, registrado_em)
SELECT 1, pg_temp.cid(para), pg_temp.cid(de), tipo, descricao, now() - make_interval(days => dias_atras)
FROM (VALUES
    ('beatriz.santos@demo.dama',  'mariana.souza@demo.dama',    'Destaque',     'Entregou a integração com o assistente de código antes do prazo', 15),
    ('rafael.oliveira@demo.dama', 'ricardo.nogueira@demo.dama', 'Inovação',     'Automatizou a conciliação de pedidos com IA',                    25),
    ('lucas.pereira@demo.dama',   'mariana.souza@demo.dama',    'Colaboração',  'Apoiou o comercial na análise de churn',                          8),
    ('juliana.rocha@demo.dama',   'ricardo.nogueira@demo.dama', 'Mentoria',     'Conduziu a trilha interna de uso responsável de IA',              40),
    ('juliana.rocha@demo.dama',   'mariana.souza@demo.dama',    'Destaque',     'Referência técnica no hackathon de automação',                    60)
) AS v(para, de, tipo, descricao, dias_atras);

-- ---------------------------------------------------------------------
-- Atividades produtivas e AI Credits
-- ---------------------------------------------------------------------
INSERT INTO atividade_produtiva (empresa_id, nome, descricao) VALUES
    (1, 'Code review assistido por IA',        'Revisão de código com sugestões de IA aceitas'),
    (1, 'Relatório gerado com IA e validado',  'Relatório produzido com IA e aprovado pelo gestor'),
    (1, 'Automação de processo com IA',        'Processo manual substituído por fluxo com IA'),
    (1, 'Prompt documentado na base',          'Prompt reutilizável publicado na base de conhecimento'),
    (1, 'Uso de IA sem validação humana',      'Registrado para acompanhamento; não gera créditos');

INSERT INTO ai_credit_regra (empresa_id, atividade_id, nome, criterio_elegibilidade, creditos_concedidos)
SELECT 1, pg_temp.aid(atividade), nome, criterio, creditos
FROM (VALUES
    ('Code review assistido por IA',       'Revisão aprovada',        'PR aprovado com sugestões de IA incorporadas', 25),
    ('Relatório gerado com IA e validado', 'Relatório aprovado',      'Relatório aprovado pelo gestor da área',       40),
    ('Automação de processo com IA',       'Automação em produção',   'Automação publicada e em uso pela equipe',     80),
    ('Prompt documentado na base',         'Prompt publicado',        'Prompt revisado por pares',                    15)
) AS v(atividade, nome, criterio, creditos);

-- cada registro de atividade elegível gera um CREDITO da regra, como na API
WITH registros AS (
    INSERT INTO atividade_registro (atividade_id, colaborador_id, quantidade, registrado_em)
    SELECT pg_temp.aid(v.atividade), pg_temp.cid(v.email), 1, now() - make_interval(days => v.dias_atras)
    FROM (VALUES
        ('juliana.rocha@demo.dama',   'Automação de processo com IA',       70),
        ('juliana.rocha@demo.dama',   'Automação de processo com IA',       20),
        ('juliana.rocha@demo.dama',   'Relatório gerado com IA e validado', 14),
        ('juliana.rocha@demo.dama',   'Relatório gerado com IA e validado', 6),
        ('juliana.rocha@demo.dama',   'Prompt documentado na base',         4),
        ('juliana.rocha@demo.dama',   'Code review assistido por IA',       2),
        ('beatriz.santos@demo.dama',  'Code review assistido por IA',       40),
        ('beatriz.santos@demo.dama',  'Code review assistido por IA',       22),
        ('beatriz.santos@demo.dama',  'Code review assistido por IA',       9),
        ('beatriz.santos@demo.dama',  'Relatório gerado com IA e validado', 16),
        ('beatriz.santos@demo.dama',  'Prompt documentado na base',         3),
        ('beatriz.santos@demo.dama',  'Prompt documentado na base',         1),
        ('lucas.pereira@demo.dama',   'Relatório gerado com IA e validado', 33),
        ('lucas.pereira@demo.dama',   'Relatório gerado com IA e validado', 11),
        ('lucas.pereira@demo.dama',   'Prompt documentado na base',         5),
        ('lucas.pereira@demo.dama',   'Prompt documentado na base',         2),
        ('rafael.oliveira@demo.dama', 'Automação de processo com IA',       24),
        ('rafael.oliveira@demo.dama', 'Relatório gerado com IA e validado', 7),
        ('rafael.oliveira@demo.dama', 'Prompt documentado na base',         3),
        ('felipe.costa@demo.dama',    'Code review assistido por IA',       18),
        ('felipe.costa@demo.dama',    'Uso de IA sem validação humana',     6),
        ('gabriel.souza@demo.dama',   'Relatório gerado com IA e validado', 12),
        ('paula.mendes@demo.dama',    'Prompt documentado na base',         26),
        ('camila.ferreira@demo.dama', 'Uso de IA sem validação humana',     15)
    ) AS v(email, atividade, dias_atras)
    RETURNING registro_id, atividade_id, colaborador_id, registrado_em
)
INSERT INTO ai_credit_transacao (colaborador_id, regra_id, registro_id, tipo, quantidade, motivo, registrado_em)
SELECT r.colaborador_id, g.regra_id, r.registro_id, 'CREDITO', g.creditos_concedidos,
       'Atividade elegível: ' || a.nome, r.registrado_em
FROM registros r
JOIN ai_credit_regra g ON g.atividade_id = r.atividade_id AND g.ativa
JOIN atividade_produtiva a ON a.atividade_id = r.atividade_id;

INSERT INTO ai_credit_transacao (colaborador_id, tipo, quantidade, motivo, registrado_em)
SELECT pg_temp.cid(email), tipo, quantidade, motivo, now() - make_interval(days => dias_atras)
FROM (VALUES
    ('juliana.rocha@demo.dama',  'DEBITO',  60, 'Acesso ao modelo avançado por uma semana', 10),
    ('beatriz.santos@demo.dama', 'DEBITO',  25, 'Acesso a assistente de código ampliado',    5),
    ('lucas.pereira@demo.dama',  'CREDITO', 20, 'Bônus da gestora pela análise de churn',    8)
) AS v(email, tipo, quantidade, motivo, dias_atras);

-- ---------------------------------------------------------------------
-- Conquistas: definição e concessão
-- ---------------------------------------------------------------------
INSERT INTO conquista (empresa_id, nome, descricao, criterio, valor_minimo) VALUES
    (1, 'Primeiros 100 pontos',     'Somou 100 pontos na plataforma',                 'PONTOS_TOTAIS',             100),
    (1, 'Meio milhar',              'Somou 500 pontos',                               'PONTOS_TOTAIS',             500),
    (1, 'Meta batida',              'Concluiu a primeira meta individual',            'METAS_CONCLUIDAS',          1),
    (1, 'Desafiante',               'Concluiu um desafio',                            'DESAFIOS_CONCLUIDOS',       1),
    (1, 'Reconhecido pelos pares',  'Recebeu um reconhecimento',                      'RECONHECIMENTOS_RECEBIDOS', 1),
    (1, 'Mão na massa',             'Registrou 5 atividades produtivas',              'ATIVIDADES_REGISTRADAS',    5),
    (1, 'Fluente em IA',            'Acumulou 150 AI Credits',                        'AI_CREDITS_ACUMULADOS',     150);
INSERT INTO conquista (empresa_id, nome, descricao, criterio) VALUES
    (1, 'Destaque do trimestre',    'Escolhido pela diretoria como destaque do trimestre', 'MANUAL');

INSERT INTO colaborador_conquista (colaborador_id, conquista_id, concedida_por_id, obtida_em)
SELECT pg_temp.cid('juliana.rocha@demo.dama'), conquista_id, pg_temp.cid('ricardo.nogueira@demo.dama'), now() - interval '9 days'
FROM conquista WHERE empresa_id = 1 AND nome = 'Destaque do trimestre';

-- mesma regra que ConquistaRepository.CONCEDER_AUTOMATICAS usa na API
INSERT INTO colaborador_conquista (colaborador_id, conquista_id)
SELECT c.colaborador_id, q.conquista_id
FROM colaborador c
JOIN conquista q ON q.empresa_id = c.empresa_id AND q.ativa AND q.criterio <> 'MANUAL'
WHERE c.ativo AND c.empresa_id = 1
  AND (CASE q.criterio
         WHEN 'PONTOS_TOTAIS' THEN (SELECT COALESCE(SUM(p.pontos), 0) FROM pontuacao p WHERE p.colaborador_id = c.colaborador_id)
         WHEN 'METAS_CONCLUIDAS' THEN (SELECT COUNT(*) FROM meta m WHERE m.colaborador_id = c.colaborador_id AND m.status = 'Concluida')
         WHEN 'DESAFIOS_CONCLUIDOS' THEN (SELECT COUNT(*) FROM desafio_participante dp WHERE dp.colaborador_id = c.colaborador_id AND dp.status = 'Concluido')
         WHEN 'RECONHECIMENTOS_RECEBIDOS' THEN (SELECT COUNT(*) FROM reconhecimento r WHERE r.colaborador_id = c.colaborador_id)
         WHEN 'ATIVIDADES_REGISTRADAS' THEN (SELECT COUNT(*) FROM atividade_registro ar WHERE ar.colaborador_id = c.colaborador_id)
         WHEN 'AI_CREDITS_ACUMULADOS' THEN (SELECT COALESCE(SUM(t.quantidade), 0) FROM ai_credit_transacao t WHERE t.colaborador_id = c.colaborador_id AND t.tipo = 'CREDITO')
       END) >= q.valor_minimo
ON CONFLICT DO NOTHING;
