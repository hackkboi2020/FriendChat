# Free 24/7 chat server on Cloudflare (no card)

This uses a Cloudflare Worker with a Durable Object. It does not sleep, and it needs no credit card on the free plan.

1. Make a free account at dash.cloudflare.com (email only).
2. Install Node.js (nodejs.org) on your PC.
3. Open a terminal in the `cloudflare` folder of this repo and run:
```
npx wrangler login
npx wrangler secret put PASSWORD
npx wrangler deploy
```
   - `login` opens your browser to approve access.
   - `secret put` asks for the shared chat password. It may first ask you to create the Worker, say yes.
   - `deploy` prints an address like `friendchat.YOURNAME.workers.dev`.
4. Open that address in a browser. It should say "Friend Chat server is running".
5. In the mod: **Config**, then
   - Server host: `friendchat.YOURNAME.workers.dev`
   - Server port: `443`
   - Use TLS (wss): **ON**
   - Password: the one you set
   - **Save & Reconnect**

Notes: the last 100 messages are kept even when the Worker is idle. Free plan limits can change, so check Cloudflare's pricing page. A small friend group is far below them.
