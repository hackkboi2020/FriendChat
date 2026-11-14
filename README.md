# Friend Chat

A sleek chat window for you and your friends in Minecraft (Fabric). It works in the main menu, in a world, and on any server, because it connects to its own small chat server instead of the Minecraft server you are playing on.

## Features

- Chat window that opens anywhere: main menu, pause menu, multiplayer list, or in game
- Message list with timestamps, coloured names, word wrap and mouse-wheel scrolling
- New messages also show in vanilla chat while the window is closed (can be turned off)
- Automatic reconnect if the connection drops
- In-game config screen: server host, port, TLS, username, password, window width and height, window position (center or any corner), opacity, auto-connect
- Free to host 24/7 (see [Chat server](#2-set-up-the-chat-server))

## Supported versions

| Minecraft | Java | Fabric API | Source folder |
|---|---|---|---|
| 1.21.11 | 21 | 0.141.4+1.21.11 | `mc-1.21.11` |
| 26.3 | 25 | 0.161.0+26.3 | `mc-26.3` |

Requires Fabric Loader 0.19.5 or newer. Client-side only: nothing needs to be installed on the Minecraft servers you join.

## 1. Install the mod

1. Install the [Fabric Loader](https://fabricmc.net/use/installer/) for your Minecraft version.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) for the same Minecraft version.
3. Download the Friend Chat jar for your Minecraft version from the **Releases** page (or from the **Actions** tab, under Artifacts of a successful run). Use the jar without `-sources` in its name.
4. Put the jar and Fabric API in your `.minecraft/mods` folder. Only use the jar that matches your Minecraft version.

## 2. Set up the chat server

Everyone who wants to chat together must use the same server. You only need one person to set it up. Pick **one** option.

### Option A: Cloudflare Workers (free, always on, no credit card)

You need a free Cloudflare account and [Node.js](https://nodejs.org) (LTS).

1. Create a free account at [dash.cloudflare.com](https://dash.cloudflare.com) (email only).
2. In the dashboard, open **Workers & Pages** once. This creates your free `workers.dev` address.
3. Open a terminal in the `cloudflare` folder of this repo. On Windows: open the folder in File Explorer, click the address bar, type `cmd` and press Enter.
4. Run these commands one at a time:
   ```
   npx wrangler login
   npx wrangler secret put PASSWORD
   npx wrangler deploy
   ```
   - `login` opens your browser. Click Allow.
   - `secret put PASSWORD` asks for the shared chat password. Type it and press Enter. Say yes if it offers to create the Worker.
   - `deploy` prints your server address, like `friendchat.YOURNAME.workers.dev`.
5. Open that address in a browser. It should say "Friend Chat server is running".

Use these values in the mod: host `friendchat.YOURNAME.workers.dev`, port `443`, **Use TLS (wss): ON**.

### Option B: Your own computer or server (Node.js)

```
cd server
npm install
PASSWORD=yoursecret PORT=8765 node server.js
```

On Windows PowerShell:

```
$env:PASSWORD="yoursecret"; $env:PORT="8765"; node server.js
```

Friends need to reach your machine: forward TCP port 8765 on your router, or use a tunnel service. In the mod use host = your public address, port `8765`, TLS **OFF** (turn TLS ON only if your address is behind HTTPS).

### Option C: Other hosts

`server/server.js` runs on any host that supports Node.js and WebSockets. A `render.yaml` is included for [Render](https://render.com), but free plans there may require a card. See `DEPLOY-RENDER.md`.

## 3. Configure the mod

1. Start the game. On the main menu, click **Chat** (top left).
2. Click **Config** and fill in:
   - **Server host / IP**: your server address, without `https://`
   - **Server port**: `443` for Cloudflare, or your own port
   - **Use TLS (wss)**: ON for Cloudflare, OFF for a plain Node server
   - **Username**: leave blank to use your Minecraft name
   - **Password**: the shared password
3. Adjust window width, height, position and opacity if you like.
4. Click **Save & Reconnect**. The status at the top of the chat window should turn green and say **Connected**.

Settings are saved in `config/friendchat.json`.

## Using it

| Action | How |
|---|---|
| Open chat in a world | Press `Y` (change it in Options > Controls > Key Binds) |
| Open chat in menus | Click the **Chat** button at the top left |
| Send a message | Type and press Enter, or click **Send** |
| Scroll history | Mouse wheel |
| Open settings | **Config** button in the chat window |
| Reconnect | **Reconnect** button in the chat window |
| Close | Esc |

## Troubleshooting

| Status or problem | What to check |
|---|---|
| Wrong password | The password must match the one set on the server (`PASSWORD` secret on Cloudflare) |
| Failed or Connection error | Host spelled correctly with no `https://`, port correct, TLS setting correct, server reachable in a browser |
| Bad server address | The host contains invalid characters or spaces |
| Stays on Connecting | Wrong port or TLS setting, or a firewall is blocking the connection |
| No **Chat** button | Make sure the jar and Fabric API match your Minecraft version, and that only one Friend Chat jar is in `mods` |
| Game crashes on start | Attach `logs/latest.log` in an issue |

## Build from source

The repo contains one Gradle project per Minecraft version. GitHub Actions builds both on every push (`.github/workflows/build.yml`) and uploads the jars as Artifacts. To publish a release with both jars:

```
git tag v1.0.0
git push origin v1.0.0
```

To build locally you need JDK 25 and Gradle 9.6:

```
cd mc-1.21.11   # or mc-26.3
gradle build
```

The jar is written to `build/libs/`.

## Project layout

```
mc-1.21.11/    Fabric mod for Minecraft 1.21.11
mc-26.3/       Fabric mod for Minecraft 26.3
cloudflare/    Chat server for Cloudflare Workers (free)
server/        Chat server for Node.js
```

## Notes

- Anyone with the server address and password can join the chat, so keep the password private.
- Messages are relayed through the chat server. Do not share anything sensitive.
- The last 100 messages are kept on the server so new joiners see recent history.
- Chat is separate from Minecraft's own chat and from any Minecraft server.

## License

Add a `LICENSE` file of your choice (the mod metadata says MIT).
