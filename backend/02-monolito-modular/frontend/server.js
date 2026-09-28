const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');
const app = express();
const PORT = process.env.PORT || 3000;
const API = process.env.API_BASE_URL || 'http://localhost:8080';
app.use('/api', createProxyMiddleware({ target: API, changeOrigin: true }));
app.use(express.static('public'));
app.listen(PORT, () => console.log(`Front em http://localhost:${PORT} -> ${API}`));
