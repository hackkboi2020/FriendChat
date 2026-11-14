# Friend Chat (Fabric)

A chat window for you and your friends that works in the main menu, in-game and on any server, because it talks to its own small chat server instead of the Minecraft server.

| Folder | Minecraft | Java | Fabric API |
|---|---|---|---|
| `mc-1.21.11` | 1.21.11 | 21 | 0.141.4+1.21.11 |
| `mc-26.3` | 26.3 | 25 | 0.161.0+26.3 |

## Build on GitHub
1. Create a repo, upload everything in this folder, push to `main`.
2. Open the **Actions** tab. Each push builds both versions. Download the jars from the run's **Artifacts**.
3. To publish a release with both jars: `git tag v1.0.0 && git push origin v1.0.0`.

## Install
Fabric Loader 0.19.5+, Fabric API for your version, and the matching jar in `mods/`. Do not install both jars.

## Chat server
```
cd server
npm install
PASSWORD=yoursecret PORT=8765 node server.js
```
Host it on a PC with the port forwarded, a VPS, or any host that runs Node and supports WebSockets. Then in the game: **Config**, set the server host/port and the password. Everyone uses the same host, port and password.

## Using it
- In game press **Y** (changeable in Controls). In the main menu, pause menu and multiplayer list use the **Chat** button, top left.
- **Config** in the chat window: server host, port, username, password, window width/height, window position (center or any corner), opacity, auto-connect, and echo of new messages into vanilla chat. Settings are also in `config/friendchat.json`.
- Mouse wheel scrolls history, Enter sends.

## If the build fails
I could not compile this myself. If Actions shows an error, send me the error text. The lines most likely to need a tweak on a given Minecraft version are the `keyPressed(KeyEvent)` override and the key-mapping registration.
