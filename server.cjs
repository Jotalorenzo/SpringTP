const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');

const app = express();

// Redirige las peticiones /api hacia Spring Boot
app.use('/api', createProxyMiddleware({
    target: 'http://localhost:8080',
    changeOrigin: true
}));

// Publica los archivos del frontend
app.use(express.static('.'));

app.listen(3000, () => {
    console.log('Frontend: http://localhost:8080');
    console.log('Proxy API: http://localhost:8080/api');
});
