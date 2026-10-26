const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 3000;
const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.apk': 'application/vnd.android.package-archive'
};

function handleRequest(req, res) {
  let reqPath = req.url.split('?')[0];

  // API routing
  if (reqPath === '/api/sync' || reqPath === '/api/sync.js') {
    const syncHandler = require('./web/api/sync');
    if (req.method === 'POST') {
      let body = '';
      req.on('data', chunk => { body += chunk.toString(); });
      req.on('end', () => {
        req.body = body;
        syncHandler(req, res);
      });
      return;
    }
    syncHandler(req, res);
    return;
  }

  if (reqPath === '/api/gemini-scan' || reqPath === '/api/gemini-scan.js') {
    const geminiHandler = require('./web/api/gemini-scan');
    let body = '';
    req.on('data', chunk => { body += chunk.toString(); });
    req.on('end', () => {
      req.body = body;
      geminiHandler(req, res);
    });
    return;
  }

  if (reqPath === '/' || reqPath === '') {
    reqPath = '/index.html';
  }

  // Strip leading /web/ if present
  if (reqPath.startsWith('/web/')) {
    reqPath = reqPath.substring(4);
  }

  const filePath = path.join(__dirname, 'web', reqPath);
  const ext = path.extname(filePath).toLowerCase();
  const contentType = MIME_TYPES[ext] || 'application/octet-stream';

  fs.readFile(filePath, (err, content) => {
    if (err) {
      if (err.code === 'ENOENT') {
        res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
        res.end('404 Not Found');
      } else {
        res.writeHead(500, { 'Content-Type': 'text/plain; charset=utf-8' });
        res.end(`Server Error: ${err.code}`);
      }
    } else {
      res.writeHead(200, {
        'Content-Type': contentType,
        'Cache-Control': ext === '.json' || ext === '.apk' ? 'no-cache, no-store, must-revalidate' : 'public, max-age=3600',
        'Access-Control-Allow-Origin': '*'
      });
      res.end(content);
    }
  });
}

const server = http.createServer(handleRequest);

module.exports = handleRequest;
module.exports.default = handleRequest;

if (require.main === module) {
  server.listen(PORT, '0.0.0.0', () => {
    console.log(`Dawaii Server running at http://localhost:${PORT}`);
  });
}
