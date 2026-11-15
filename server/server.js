// Friend Chat relay server. Run: npm install && PASSWORD=secret PORT=8765 node server.js
const http = require('http');
const { WebSocketServer } = require('ws');
const PORT = parseInt(process.env.PORT || '8765', 10);
const PASSWORD = process.env.PASSWORD || '';
const HISTORY = 100;
const history = [];
const server = http.createServer((req, res) => {
  res.writeHead(200, { 'Content-Type': 'text/plain' });
  res.end('Friend Chat server is running');
});
const wss = new WebSocketServer({ server });

function broadcast(obj) {
  const s = JSON.stringify(obj);
  for (const c of wss.clients) if (c.readyState === 1) c.send(s);
}
function clean(s, n) { return String(s || '').replace(/[\u0000-\u001f]/g, ' ').trim().slice(0, n); }

wss.on('connection', (ws, req) => {
  const q = new URL(req.url, 'http://x').searchParams;
  if (PASSWORD && q.get('pw') !== PASSWORD) { ws.close(4001, 'bad password'); return; }
  ws.user = clean(q.get('name'), 32) || 'Player';
  ws.alive = true;
  ws.stamps = [];
  ws.on('pong', () => { ws.alive = true; });
  for (const m of history) ws.send(JSON.stringify({ ...m, h: true }));
  broadcast({ type: 'system', text: ws.user + ' joined' });

  ws.on('message', (data) => {
    let o; try { o = JSON.parse(data.toString()); } catch { return; }
    if (!o || o.type !== 'chat') return;
    const now = Date.now();
    ws.stamps = ws.stamps.filter((t) => now - t < 3000);
    if (ws.stamps.length >= 6) return;
    ws.stamps.push(now);
    const text = clean(o.text, 256);
    if (!text) return;
    const color = /^#[0-9a-fA-F]{6}$/.test(o.color || '') ? o.color : ''; const icon = Array.from(String(o.icon || '').replace(/[\u0000-\u001f]/g, '')).slice(0, 2).join('');
    const msg = { type: 'chat', user: ws.user, text, ts: now, color, icon };
    history.push(msg); if (history.length > HISTORY) history.shift();
    broadcast(msg);
  });
  ws.on('close', () => broadcast({ type: 'system', text: ws.user + ' left' }));
});

setInterval(() => {
  for (const c of wss.clients) {
    if (!c.alive) { c.terminate(); continue; }
    c.alive = false; c.ping();
  }
}, 30000);
server.listen(PORT, () => console.log('Friend Chat server on port ' + PORT + (PASSWORD ? ' (password protected)' : ' (no password)')));
