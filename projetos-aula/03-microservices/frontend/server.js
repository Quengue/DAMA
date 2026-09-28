const express=require('express'); const {createProxyMiddleware}=require('http-proxy-middleware'); const app=express();
const PROJECT=process.env.PROJECT_URL||'http://localhost:8081'; const USAGE=process.env.USAGE_URL||'http://localhost:8082';
app.use('/api/projects',createProxyMiddleware({target:PROJECT,changeOrigin:true})); app.use('/api/usages',createProxyMiddleware({target:USAGE,changeOrigin:true})); app.use(express.static('public')); app.listen(3000,()=>console.log('BFF/front :3000'));
