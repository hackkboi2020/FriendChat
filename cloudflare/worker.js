// Friend Chat on Cloudflare Workers + Durable Objects (free plan, no card).
export class ChatRoom {
  constructor(state, env) {
    this.state = state;
    this.env = env;
  }

  clean(s, n) {
    return String(s || '').replace(/[\u0000-\u001f]/g, ' ').trim().slice(0, n);
  }

  broadcast(obj) {
    const s = JSON.stringify(obj);
    for (const ws of this.state.getWebSockets()) {
      try { ws.send(s); } catch (e) {}
    }
  }

  async fetch(request) {
    if (request.headers.get('Upgrade') !== 'websocket') {
      return new Response('Friend Chat server is running');
    }
    const url = new URL(request.url);
    const pair = new WebSocketPair();
    const [client, server] = Object.values(pair);
    const password = this.env.PASSWORD || '';
    if (password && url.searchParams.get('pw') !== password) {
      server.accept();
      server.close(4001, 'bad password');
      return new Response(null, { status: 101, webSocket: client });
    }
    const user = this.clean(url.searchParams.get('name'), 32) || 'Player';
    this.state.acceptWebSocket(server);
    server.serializeAttachment({ user, stamps: [] });
    const history = (await this.state.storage.get('history')) || [];
    for (const m of history) server.send(JSON.stringify({ ...m, h: true }));
    this.broadcast({ type: 'system', text: user + ' joined' });
    return new Response(null, { status: 101, webSocket: client });
  }

  async webSocketMessage(ws, data) {
    let o;
    try { o = JSON.parse(typeof data === 'string' ? data : new TextDecoder().decode(data)); } catch (e) { return; }
    if (!o || o.type !== 'chat') return;
    const att = ws.deserializeAttachment() || { user: 'Player', stamps: [] };
    const now = Date.now();
    att.stamps = (att.stamps || []).filter((t) => now - t < 3000);
    if (att.stamps.length >= 6) { ws.serializeAttachment(att); return; }
    att.stamps.push(now);
    ws.serializeAttachment(att);
    const text = this.clean(o.text, 256);
    if (!text) return;
    const color = /^#[0-9a-fA-F]{6}$/.test(o.color || '') ? o.color : ''; const icon = Array.from(String(o.icon || '').replace(/[\u0000-\u001f]/g, '')).slice(0, 2).join('');
    const msg = { type: 'chat', user: att.user, text, ts: now, color, icon };
    const history = (await this.state.storage.get('history')) || [];
    history.push(msg);
    while (history.length > 100) history.shift();
    await this.state.storage.put('history', history);
    this.broadcast(msg);
  }

  async webSocketClose(ws, code, reason) {
    const att = ws.deserializeAttachment();
    try { ws.close(code === 1005 || code === 1006 ? 1000 : code, reason); } catch (e) {}
    if (att && att.user) this.broadcast({ type: 'system', text: att.user + ' left' });
  }

  async webSocketError(ws) {
    try { ws.close(1011, 'error'); } catch (e) {}
  }
}

export default {
  async fetch(request, env) {
    const id = env.CHAT.idFromName('main');
    return env.CHAT.get(id).fetch(request);
  },
};
