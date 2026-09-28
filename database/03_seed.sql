-- =====================================================================
-- Seed mínimo: perfis e permissões padrão (RF18) + uma empresa/departamento
-- de exemplo para permitir subir e testar a API sem cadastro manual prévio.
-- =====================================================================
SET search_path = dama, public;

INSERT INTO permissao (codigo, descricao) VALUES
    ('COLABORADOR_LER',      'Consultar colaboradores'),
    ('COLABORADOR_ESCREVER', 'Cadastrar e editar colaboradores'),
    ('PERFIL_GERENCIAR',     'Atribuir perfis e permissões'),
    ('DESEMPENHO_LER',       'Consultar indicadores, metas e dashboard'),
    ('DESEMPENHO_ESCREVER',  'Registrar indicadores e metas'),
    ('GAMIFICACAO_LER',      'Consultar pontuação, ranking, desafios e reconhecimentos'),
    ('GAMIFICACAO_ESCREVER', 'Lançar pontos, desafios e reconhecimentos'),
    ('AI_CREDITS_LER',       'Consultar saldo e histórico de AI Credits'),
    ('AI_CREDITS_GERENCIAR', 'Conceder/debitar AI Credits e gerenciar regras'),
    ('ANALYTICS_LER',        'Consultar analytics consolidado e comparação entre equipes');

INSERT INTO perfil (nome, descricao) VALUES
    ('Administrador', 'Acesso completo à plataforma'),
    ('Gestor',         'Gestão de desempenho e gamificação da própria empresa'),
    ('Colaborador',    'Consulta ao próprio desempenho, pontuação e AI Credits');

-- Administrador: todas as permissões
INSERT INTO perfil_permissao (perfil_id, permissao_id)
SELECT (SELECT perfil_id FROM perfil WHERE nome = 'Administrador'), permissao_id FROM permissao;

-- Gestor: tudo, exceto gerenciar perfis/permissões de acesso
INSERT INTO perfil_permissao (perfil_id, permissao_id)
SELECT (SELECT perfil_id FROM perfil WHERE nome = 'Gestor'), permissao_id
FROM permissao WHERE codigo <> 'PERFIL_GERENCIAR';

-- Colaborador: apenas leitura do próprio desempenho/gamificação/créditos
INSERT INTO perfil_permissao (perfil_id, permissao_id)
SELECT (SELECT perfil_id FROM perfil WHERE nome = 'Colaborador'), permissao_id
FROM permissao WHERE codigo IN ('DESEMPENHO_LER','GAMIFICACAO_LER','AI_CREDITS_LER');

-- Empresa e departamento de exemplo (facilita testar a API local/Docker)
INSERT INTO empresa (nome, setor, porte) VALUES ('Empresa Demonstração', 'Tecnologia', 'Media');

INSERT INTO departamento (empresa_id, nome) VALUES
    ((SELECT empresa_id FROM empresa WHERE nome = 'Empresa Demonstração'), 'Operações'),
    ((SELECT empresa_id FROM empresa WHERE nome = 'Empresa Demonstração'), 'Recursos Humanos');

-- Colaborador administrador de bootstrap: sem ele, ninguém consegue autenticar
-- (nem para cadastrar o primeiro colaborador de verdade) numa base recém-criada.
-- Ver README "Como testar" para o X-Colaborador-Id a usar.
INSERT INTO colaborador (empresa_id, departamento_id, nome, email, cargo, senioridade, data_admissao)
VALUES (
    (SELECT empresa_id FROM empresa WHERE nome = 'Empresa Demonstração'),
    (SELECT departamento_id FROM departamento WHERE nome = 'Recursos Humanos'
        AND empresa_id = (SELECT empresa_id FROM empresa WHERE nome = 'Empresa Demonstração')),
    'Administrador da Plataforma', 'admin@empresa-demonstracao.com', 'Administrador de Sistema', 'Especialista', CURRENT_DATE
);

INSERT INTO colaborador_perfil (colaborador_id, perfil_id)
SELECT
    (SELECT colaborador_id FROM colaborador WHERE email = 'admin@empresa-demonstracao.com'),
    (SELECT perfil_id FROM perfil WHERE nome = 'Administrador');

-- Níveis padrão de autonomia de acesso à IA (RF25/RF26) e de evolução (RF31).
-- Podem ser ajustados pela API (POST /api/ai-credits/niveis e /api/niveis-evolucao).
INSERT INTO ai_nivel_autonomia (nome, creditos_minimos, ordem, descricao) VALUES
    ('Básico',        0,   1, 'Ferramentas de IA aprovadas com dados públicos'),
    ('Ampliado',      100, 2, 'Uso com dados internos não sensíveis'),
    ('Avançado',      250, 3, 'Integrações e automações com IA no fluxo de trabalho'),
    ('Total',         500, 4, 'Acesso completo às ferramentas homologadas');

INSERT INTO nivel_evolucao (nome, pontos_minimos, ordem, descricao) VALUES
    ('Iniciante',     0,    1, 'Começando a jornada na plataforma'),
    ('Aprendiz',      100,  2, 'Primeiras entregas reconhecidas'),
    ('Praticante',    300,  3, 'Desempenho consistente'),
    ('Especialista',  700,  4, 'Referência técnica na equipe'),
    ('Referência',    1500, 5, 'Referência para toda a empresa');
