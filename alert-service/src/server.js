'use strict';

const http = require('node:http');
const { avaliar } = require('./regras');

const PORT = Number(process.env.PORT || 4000);
const LIMITE_CORPO = 2 * 1024 * 1024;
const MOTOR = 'alert-service-node';

class ErroDeEntrada extends Error {
  constructor(mensagem, status = 400) {
    super(mensagem);
    this.status = status;
  }
}

function responder(res, status, corpo) {
  const texto = JSON.stringify(corpo);
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(texto),
  });
  res.end(texto);
}

function lerCorpo(req) {
  return new Promise((resolve, reject) => {
    const partes = [];
    let tamanho = 0;
    req.on('data', (parte) => {
      tamanho += parte.length;
      if (tamanho > LIMITE_CORPO) {
        reject(new ErroDeEntrada('Corpo da requisição acima do limite permitido.', 413));
        req.destroy();
        return;
      }
      partes.push(parte);
    });
    req.on('end', () => resolve(Buffer.concat(partes).toString('utf8')));
    req.on('error', reject);
  });
}

function lista(entrada, campo) {
  const valor = entrada[campo];
  if (valor === undefined || valor === null) {
    return [];
  }
  if (!Array.isArray(valor)) {
    throw new ErroDeEntrada(`O campo "${campo}" deve ser uma lista.`);
  }
  return valor;
}

/** Valida o contrato mínimo e normaliza as listas ausentes para vazio. */
function validar(entrada) {
  if (entrada === null || typeof entrada !== 'object' || Array.isArray(entrada)) {
    throw new ErroDeEntrada('O corpo deve ser um objeto JSON.');
  }
  if (typeof entrada.dataReferencia !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(entrada.dataReferencia)) {
    throw new ErroDeEntrada('O campo "dataReferencia" é obrigatório no formato AAAA-MM-DD.');
  }
  const normalizada = {
    dataReferencia: entrada.dataReferencia,
    metas: lista(entrada, 'metas'),
    tendencias: lista(entrada, 'tendencias'),
    equipes: lista(entrada, 'equipes'),
    colaboradoresSemAtividade: lista(entrada, 'colaboradoresSemAtividade'),
  };
  normalizada.metas.forEach((m, i) => {
    for (const campo of ['descricao', 'valorAlvo', 'dataInicio', 'dataFim']) {
      if (m?.[campo] === undefined || m?.[campo] === null) {
        throw new ErroDeEntrada(`metas[${i}]: o campo "${campo}" é obrigatório.`);
      }
    }
  });
  normalizada.tendencias.forEach((t, i) => {
    if (!t?.anterior || !t?.atual) {
      throw new ErroDeEntrada(`tendencias[${i}]: informe "anterior" e "atual".`);
    }
    for (const lado of ['anterior', 'atual']) {
      for (const campo of ['valor', 'periodo', 'indicadorNome']) {
        if (t[lado][campo] === undefined || t[lado][campo] === null) {
          throw new ErroDeEntrada(`tendencias[${i}].${lado}: o campo "${campo}" é obrigatório.`);
        }
      }
    }
  });
  normalizada.equipes.forEach((e, i) => {
    for (const campo of ['nome', 'colaboradoresAtivos', 'pontosTotais']) {
      if (e?.[campo] === undefined || e?.[campo] === null) {
        throw new ErroDeEntrada(`equipes[${i}]: o campo "${campo}" é obrigatório.`);
      }
    }
  });
  normalizada.colaboradoresSemAtividade.forEach((c, i) => {
    if (c?.nome === undefined || c?.nome === null) {
      throw new ErroDeEntrada(`colaboradoresSemAtividade[${i}]: o campo "nome" é obrigatório.`);
    }
  });
  return normalizada;
}

async function tratar(req, res) {
  const { pathname } = new URL(req.url, 'http://localhost');

  if (req.method === 'GET' && pathname === '/health') {
    return responder(res, 200, { status: 'UP', servico: MOTOR });
  }

  if (pathname === '/avaliar') {
    if (req.method !== 'POST') {
      res.setHeader('Allow', 'POST');
      return responder(res, 405, { status: 405, message: 'Use POST em /avaliar.' });
    }
    let entrada;
    try {
      entrada = JSON.parse(await lerCorpo(req));
    } catch (err) {
      if (err instanceof ErroDeEntrada) throw err;
      throw new ErroDeEntrada('JSON inválido.');
    }
    const dados = validar(entrada);
    const alertas = avaliar(dados);
    console.log(`[alert-service] POST /avaliar metas=${dados.metas.length} tendencias=${dados.tendencias.length} `
      + `equipes=${dados.equipes.length} semAtividade=${dados.colaboradoresSemAtividade.length} -> ${alertas.length} alerta(s)`);
    return responder(res, 200, { motor: MOTOR, alertas });
  }

  return responder(res, 404, { status: 404, message: 'Rota inexistente.' });
}

function criarServidor() {
  return http.createServer((req, res) => {
    tratar(req, res).catch((err) => {
      if (err instanceof ErroDeEntrada) {
        return responder(res, err.status, { status: err.status, message: err.message });
      }
      console.error('[alert-service] erro inesperado:', err);
      return responder(res, 500, { status: 500, message: 'Erro interno no serviço de alertas.' });
    });
  });
}

if (require.main === module) {
  criarServidor().listen(PORT, () => {
    console.log(`alert-service em http://localhost:${PORT} (POST /avaliar, GET /health)`);
  });
}

module.exports = { criarServidor, validar };
