package dev.friendchat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ChatClient {
    public static final ChatClient INSTANCE = new ChatClient();

    private final HttpClient http = HttpClient.newHttpClient();
    private final List<ChatMessage> messages = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "FriendChat-reconnect");
        t.setDaemon(true);
        return t;
    });
    private volatile WebSocket socket;
    private volatile String status = "Disconnected";
    private volatile int generation = 0;

    private ChatClient() {
    }

    public String status() {
        return status;
    }

    public boolean isConnected() {
        return socket != null;
    }

    public List<ChatMessage> snapshot(int max) {
        synchronized (messages) {
            return new ArrayList<>(messages.subList(Math.max(0, messages.size() - max), messages.size()));
        }
    }

    public void addSystem(String text) {
        add(new ChatMessage(System.currentTimeMillis(), "", text, true));
    }

    private void add(ChatMessage m) {
        synchronized (messages) {
            messages.add(m);
            while (messages.size() > 500) messages.remove(0);
        }
    }

    public synchronized void connect() {
        disconnect();
        ChatConfig c = ChatConfig.get();
        final int gen = ++generation;
        try {
            String url = (c.useTls ? "wss" : "ws") + "://" + c.serverHost.trim() + ":" + c.serverPort
                    + "/?name=" + enc(c.effectiveName()) + "&pw=" + enc(c.password);
            status = "Connecting...";
            http.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(8))
                    .buildAsync(URI.create(url), new Listener(gen))
                    .whenComplete((w, err) -> {
                        if (err != null && gen == generation) {
                            Throwable root = err;
                            while (root.getCause() != null) root = root.getCause();
                            status = "Failed: " + root.getClass().getSimpleName();
                            scheduleReconnect(gen);
                        }
                    });
        } catch (Exception e) {
            status = "Bad server address";
        }
    }

    public synchronized void disconnect() {
        generation++;
        WebSocket w = socket;
        socket = null;
        if (w != null) {
            try {
                w.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
            } catch (Exception ignored) {
            }
        }
        status = "Disconnected";
    }

    public void shutdown() {
        disconnect();
        scheduler.shutdownNow();
    }

    public boolean send(String text) {
        WebSocket w = socket;
        if (w == null) return false;
        JsonObject o = new JsonObject();
        o.addProperty("type", "chat");
        o.addProperty("text", text);
        w.sendText(o.toString(), true);
        return true;
    }

    private void scheduleReconnect(int gen) {
        if (!ChatConfig.get().autoConnect) return;
        try {
            scheduler.schedule(() -> {
                if (gen == generation) connect();
            }, 5, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private void handle(String raw) {
        try {
            JsonObject o = JsonParser.parseString(raw).getAsJsonObject();
            String type = o.has("type") ? o.get("type").getAsString() : "chat";
            String text = o.has("text") ? o.get("text").getAsString() : "";
            if (type.equals("system")) {
                add(new ChatMessage(System.currentTimeMillis(), "", text, true));
                return;
            }
            String user = o.has("user") ? o.get("user").getAsString() : "?";
            long ts = o.has("ts") ? o.get("ts").getAsLong() : System.currentTimeMillis();
            boolean history = o.has("h") && o.get("h").getAsBoolean();
            add(new ChatMessage(ts, user, text, false));
            echo(user, text, history);
        } catch (Exception ignored) {
        }
    }

    private void echo(String user, String text, boolean history) {
        if (history || !ChatConfig.get().echoToGameChat) return;
        if (user.equals(ChatConfig.get().effectiveName())) return;
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null && !(mc.screen instanceof FriendChatScreen)) {
                mc.player.sendSystemMessage(Component.literal("\u00a79[FriendChat] \u00a7b" + user + "\u00a7r: " + text));
            }
        });
    }

    private final class Listener implements WebSocket.Listener {
        private final int gen;
        private final StringBuilder buf = new StringBuilder();

        Listener(int gen) {
            this.gen = gen;
        }

        @Override
        public void onOpen(WebSocket w) {
            if (gen != generation) {
                w.abort();
                return;
            }
            socket = w;
            status = "Connected";
            w.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket w, CharSequence data, boolean last) {
            buf.append(data);
            if (last) {
                String s = buf.toString();
                buf.setLength(0);
                if (gen == generation) handle(s);
            }
            w.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket w, int code, String reason) {
            if (gen == generation) {
                socket = null;
                if (code == 4001) {
                    status = "Wrong password";
                } else {
                    status = "Disconnected";
                    scheduleReconnect(gen);
                }
            }
            return null;
        }

        @Override
        public void onError(WebSocket w, Throwable t) {
            if (gen == generation) {
                socket = null;
                status = "Connection error";
                scheduleReconnect(gen);
            }
        }
    }
}
