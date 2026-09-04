const express = require('express');
const { Pool } = require('pg');
const crypto = require('crypto');

const app = express();
app.use(express.json());

const PORT = Number(process.env.PORT || 3001);
const pool = new Pool({
  host: process.env.DB_HOST || 'localhost',
  port: Number(process.env.DB_PORT || 5435),
  database: process.env.DB_NAME || 'alerts',
  user: process.env.DB_USER || 'ia',
  password: process.env.DB_PASSWORD || 'ia'
});

async function initDb() {
  await pool.query(`
    CREATE TABLE IF NOT EXISTS alerts (
      id UUID PRIMARY KEY,
      usage_id UUID,
      project_id UUID NOT NULL,
      level VARCHAR(20) NOT NULL,
      message VARCHAR(500) NOT NULL,
      created_at TIMESTAMPTZ NOT NULL
    )
  `);
}

function evaluate(tokens, model) {
  if (tokens >= 10000) {
    return { level: 'CRITICAL', message: `Consumo crítico de ${tokens} tokens no modelo ${model}` };
  }
  if (tokens >= 5000) {
    return { level: 'WARNING', message: `Atenção: consumo elevado de ${tokens} tokens no modelo ${model}` };
  }
  return { level: 'INFO', message: `Consumo normal de ${tokens} tokens no modelo ${model}` };
}

app.get('/health', async (_req, res) => {
  try {
    await pool.query('SELECT 1');
    res.json({ status: 'UP', service: 'alert-service' });
  } catch (e) {
    res.status(503).json({ status: 'DOWN', detail: e.message });
  }
});

app.post('/api/alerts/evaluate', async (req, res) => {
  const { usageId, projectId, tokens, model } = req.body || {};
  if (!projectId || !Number.isFinite(Number(tokens)) || !model) {
    return res.status(400).json({ error: 'projectId, tokens e model são obrigatórios' });
  }

  const result = evaluate(Number(tokens), model);
  const alert = {
    id: crypto.randomUUID(),
    usageId: usageId || null,
    projectId,
    level: result.level,
    message: result.message,
    createdAt: new Date().toISOString()
  };

  await pool.query(
    `INSERT INTO alerts(id, usage_id, project_id, level, message, created_at)
     VALUES($1,$2,$3,$4,$5,$6)`,
    [alert.id, alert.usageId, alert.projectId, alert.level, alert.message, alert.createdAt]
  );

  console.log(`[alert-service] ${alert.level} project=${projectId} tokens=${tokens}`);
  res.status(201).json(alert);
});

app.get('/api/alerts', async (_req, res) => {
  const { rows } = await pool.query(
    'SELECT id, usage_id AS "usageId", project_id AS "projectId", level, message, created_at AS "createdAt" FROM alerts ORDER BY created_at DESC'
  );
  res.json(rows);
});

initDb()
  .then(() => app.listen(PORT, () => console.log(`alert-service :${PORT}`)))
  .catch(err => {
    console.error('Falha ao iniciar alert-service:', err);
    process.exit(1);
  });
