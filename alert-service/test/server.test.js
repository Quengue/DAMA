'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { criarServidor } = require('../src/server');

let servidor;
let base;

test.before(async () => {
  servidor = criarServidor();
  await new Promise((resolve) => servidor.listen(0, '127.0.0.1', resolve));
  base = `http://127.0.0.1:${servidor.address().port}`;
});

test.after(() => new Promise((resolve) => servidor.close(resolve)));

function postar(corpo, textoBruto) {
  return fetch(`${base}/avaliar`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: textoBruto ?? JSON.stringify(corpo),
  });
}

test('GET /health responde UP', async () => {
  const resposta = await fetch(`${base}/health`);
  assert.equal(resposta.status, 200);
  assert.equal((await resposta.json()).status, 'UP');
});

test('POST /avaliar devolve os alertas calculados', async () => {
  const resposta = await postar({
    dataReferencia: '2026-09-28',
    metas: [{
      metaId: 1, descricao: 'Entregas no prazo', colaboradorId: 10, colaboradorNome: 'Lucas',
      valorAlvo: 100, progressoAtual: 40, dataInicio: '2026-07-30', dataFim: '2026-09-27', maiorMelhor: true,
    }],
  });
  assert.equal(resposta.status, 200);
  const corpo = await resposta.json();
  assert.equal(corpo.motor, 'alert-service-node');
  assert.equal(corpo.alertas.length, 1);
  assert.equal(corpo.alertas[0].tipo, 'META_VENCIDA');
  assert.equal(corpo.alertas[0].severidade, 'ALTA');
});

test('listas ausentes são tratadas como vazias', async () => {
  const resposta = await postar({ dataReferencia: '2026-09-28' });
  assert.equal(resposta.status, 200);
  assert.deepEqual((await resposta.json()).alertas, []);
});

test('entrada sem dataReferencia é rejeitada com 400', async () => {
  const resposta = await postar({ metas: [] });
  assert.equal(resposta.status, 400);
  assert.match((await resposta.json()).message, /dataReferencia/);
});

test('meta sem campo obrigatório é rejeitada com 400 e aponta o campo', async () => {
  const resposta = await postar({ dataReferencia: '2026-09-28', metas: [{ descricao: 'x' }] });
  assert.equal(resposta.status, 400);
  assert.match((await resposta.json()).message, /metas\[0\].*valorAlvo/);
});

test('JSON inválido é rejeitado com 400', async () => {
  const resposta = await postar(null, '{nao eh json');
  assert.equal(resposta.status, 400);
});

test('rota inexistente devolve 404 e GET em /avaliar devolve 405', async () => {
  assert.equal((await fetch(`${base}/nada`)).status, 404);
  assert.equal((await fetch(`${base}/avaliar`)).status, 405);
});
