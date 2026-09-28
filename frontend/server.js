const express = require('express');
const path = require('path');

const PORT = process.env.PORT || 3000;
const API = (process.env.API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const HEADER_USUARIO = 'X-Colaborador-Id';

const app = express();
app.disable('x-powered-by');
app.use(express.json({ limit: '1mb' }));

// Saúde do front e do backend: usado pelo healthcheck do docker-compose e para diagnóstico.
app.get('/health', async (_req, res) => {
  try {
    const resposta = await fetch(`${API}/actuator/health`);
    res.status(resposta.ok ? 200 : 503).json({ status: resposta.ok ? 'UP' : 'DOWN', backend: resposta.status });
  } catch (err) {
    res.status(503).json({ status: 'DOWN', detail: err.message });
  }
});

// BFF: repassa /api/* ao Spring Boot preservando o caminho e o cabeçalho de identificação do usuário.
app.use('/api', async (req, res) => {
  const headers = { Accept: 'application/json' };
  const usuario = req.get(HEADER_USUARIO);
  if (usuario) {
    headers[HEADER_USUARIO] = usuario;
  }
  const opcoes = { method: req.method, headers };
  if (!['GET', 'HEAD'].includes(req.method) && req.body !== undefined) {
    headers['Content-Type'] = 'application/json';
    opcoes.body = JSON.stringify(req.body);
  }

  try {
    const resposta = await fetch(`${API}${req.originalUrl}`, opcoes);
    const corpo = await resposta.text();
    res.status(resposta.status);
    const tipo = resposta.headers.get('content-type');
    if (tipo) {
      res.set('Content-Type', tipo);
    }
    res.send(corpo);
  } catch (err) {
    console.error(`[BFF] ${req.method} ${req.originalUrl}: backend indisponível (${err.message})`);
    res.status(502).json({ status: 502, message: 'Backend indisponível. Verifique se o serviço está no ar.' });
  }
});

app.use('/vendor', express.static(path.join(__dirname, 'node_modules', 'chart.js', 'dist')));
app.use(express.static(path.join(__dirname, 'public')));

app.listen(PORT, () => {
  console.log(`DAMA Intelligence em http://localhost:${PORT} (API: ${API})`);
});
