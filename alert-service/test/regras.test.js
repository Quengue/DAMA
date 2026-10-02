'use strict';

// Os casos espelham AnaliseGestaoRegrasTest.java: o serviço Node e o fallback local do backend devem concordar.
const test = require('node:test');
const assert = require('node:assert/strict');
const { avaliar, avaliarMeta, avaliarTendencia, avaliarEquipes, semAtividade } = require('../src/regras');

const HOJE = '2026-09-28';

function somarDias(data, dias) {
  const d = new Date(`${data}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + dias);
  return d.toISOString().slice(0, 10);
}

function meta(alvo, progresso, inicio, fim, maiorMelhor) {
  return {
    metaId: 1, descricao: 'Entregas no prazo', colaboradorId: 10, colaboradorNome: 'Lucas',
    departamentoId: null, departamentoNome: null, valorAlvo: alvo, progressoAtual: progresso,
    dataInicio: inicio, dataFim: fim, maiorMelhor,
  };
}

function valor(v, periodo, maiorMelhor) {
  return {
    indicadorId: 5, indicadorNome: 'Produtividade', unidade: '%', maiorMelhor, colaboradorId: 10,
    colaboradorNome: 'Lucas', departamentoId: null, departamentoNome: null, periodo, valor: v,
  };
}

test('meta com prazo encerrado é vencida com severidade alta', () => {
  const a = avaliarMeta(meta(100, 40, somarDias(HOJE, -60), somarDias(HOJE, -1), true), HOJE);
  assert.equal(a.tipo, 'META_VENCIDA');
  assert.equal(a.severidade, 'ALTA');
  assert.equal(a.colaboradorId, 10);
  assert.equal(a.referenciaId, 1);
  assert.match(a.detalhe, /Prazo terminou em 27\/09\/2026 com 40 de 100 \(Lucas\)\./);
});

test('meta atrasada em relação ao tempo decorrido fica em risco', () => {
  // 80% do prazo decorrido e só 30% do alvo atingido
  const a = avaliarMeta(meta(100, 30, somarDias(HOJE, -79), somarDias(HOJE, 20), true), HOJE);
  assert.equal(a.tipo, 'META_EM_RISCO');
  assert.equal(a.severidade, 'MEDIA');
  assert.match(a.detalhe, /80% do prazo decorrido e 30% do alvo atingido/);
});

test('meta no ritmo esperado não gera alerta', () => {
  assert.equal(avaliarMeta(meta(100, 70, somarDias(HOJE, -79), somarDias(HOJE, 20), true), HOJE), null);
  // antes da metade do prazo nunca é considerada em risco
  assert.equal(avaliarMeta(meta(100, null, somarDias(HOJE, -10), somarDias(HOJE, 60), true), HOJE), null);
});

test('meta de indicador menor-é-melhor não entra na regra de risco', () => {
  assert.equal(avaliarMeta(meta(5, 30, somarDias(HOJE, -79), somarDias(HOJE, 20), false), HOJE), null);
});

test('queda de 20% é média e de 30% é alta', () => {
  const media = avaliarTendencia(valor(80, '2026-08-28', true), valor(64, HOJE, true));
  const alta = avaliarTendencia(valor(80, '2026-08-28', true), valor(56, HOJE, true));
  assert.equal(media.severidade, 'MEDIA');
  assert.equal(alta.severidade, 'ALTA');
  assert.match(alta.titulo, /30%/);
  assert.equal(alta.detalhe, 'De 80% em 08/2026 para 56% em 09/2026.');
});

test('variação pequena ou melhora não gera alerta', () => {
  assert.equal(avaliarTendencia(valor(80, '2026-08-28', true), valor(75, HOJE, true)), null);
  assert.equal(avaliarTendencia(valor(80, '2026-08-28', true), valor(95, HOJE, true)), null);
});

test('indicador menor-é-melhor piora quando sobe', () => {
  const a = avaliarTendencia(valor(10, '2026-08-28', false), valor(13, HOJE, false));
  assert.equal(a.tipo, 'INDICADOR_EM_QUEDA');
  assert.equal(a.severidade, 'ALTA');
  assert.equal(avaliarTendencia(valor(10, '2026-08-28', false), valor(7, HOJE, false)), null);
});

test('limites exatos de 10% e 25% de piora contam como alerta', () => {
  assert.equal(avaliarTendencia(valor(100, '2026-08-28', true), valor(90, HOJE, true)).severidade, 'MEDIA');
  assert.equal(avaliarTendencia(valor(100, '2026-08-28', true), valor(75, HOJE, true)).severidade, 'ALTA');
});

test('equipe com menos da metade da média da empresa gera alerta', () => {
  const alertas = avaliarEquipes([
    { departamentoId: 1, nome: 'Tecnologia', colaboradoresAtivos: 4, pontosTotais: 800 },
    { departamentoId: 2, nome: 'Comercial', colaboradoresAtivos: 4, pontosTotais: 100 },
    { departamentoId: 3, nome: 'Vazia', colaboradoresAtivos: 0, pontosTotais: 0 },
  ]);
  // média da empresa = 900 / 8 = 112,5; Comercial = 25 < 56,25
  assert.deepEqual(alertas.map((a) => a.departamentoId), [2]);
  assert.match(alertas[0].detalhe, /Média de 25 pontos por colaborador ativo, contra 112,5 na empresa\./);
});

test('sem pontos na empresa não há comparação entre equipes', () => {
  assert.deepEqual(avaliarEquipes([
    { departamentoId: 1, nome: 'A', colaboradoresAtivos: 3, pontosTotais: 0 },
    { departamentoId: 2, nome: 'B', colaboradoresAtivos: 2, pontosTotais: 0 },
  ]), []);
});

test('colaborador sem atividade gera alerta de severidade baixa com a data de corte', () => {
  const a = semAtividade({ colaboradorId: 7, nome: 'Marina', departamentoId: 2 }, HOJE);
  assert.equal(a.severidade, 'BAIXA');
  assert.equal(a.titulo, 'Marina sem atividade nos últimos 30 dias');
  assert.match(a.detalhe, /desde 29\/08\/2026\./);
});

test('avaliar mantém a ordem metas, tendências, equipes e sem atividade', () => {
  const alertas = avaliar({
    dataReferencia: HOJE,
    metas: [meta(100, 40, somarDias(HOJE, -60), somarDias(HOJE, -1), true)],
    tendencias: [{ anterior: valor(80, '2026-08-28', true), atual: valor(56, HOJE, true) }],
    equipes: [],
    colaboradoresSemAtividade: [{ colaboradorId: 7, nome: 'Marina', departamentoId: 2 }],
  });
  assert.deepEqual(alertas.map((a) => a.tipo), ['META_VENCIDA', 'INDICADOR_EM_QUEDA', 'SEM_ATIVIDADE_RECENTE']);
});
