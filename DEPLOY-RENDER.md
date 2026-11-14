# Free 24/7 hosting on Render

1. Push this repo to GitHub.
2. Sign up at render.com with GitHub.
3. **New > Blueprint**, pick this repo. Render reads `render.yaml`. When asked, enter a `PASSWORD` (everyone in the chat uses it). Choose the **Free** plan.
   (No blueprint? **New > Web Service**, root directory `server`, build `npm install`, start `node server.js`, add env var `PASSWORD`.)
4. After it deploys you get an address like `friendchat-server.onrender.com`. Opening it in a browser should say "Friend Chat server is running".
5. Free services sleep after about 15 minutes without traffic. To keep it awake, make a free monitor at uptimerobot.com: type HTTP(s), URL `https://YOUR-APP.onrender.com/`, interval 5 minutes.
6. In the mod: **Config**, then
   - Server host: `YOUR-APP.onrender.com`
   - Server port: `443`
   - Use TLS (wss): **ON**
   - Password: the one you set
   - **Save & Reconnect**

Notes: chat history is kept in memory, so it resets when the server restarts. Free-tier limits can change, so check Render's pricing page.
