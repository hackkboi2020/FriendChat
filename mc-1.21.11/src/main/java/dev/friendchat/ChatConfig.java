package dev.friendchat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;

public class ChatConfig {
    public static final String[] LAYOUTS = {"CENTER", "TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT"};
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ChatConfig instance = new ChatConfig();

    public String serverHost = "localhost";
    public int serverPort = 8765;
    public boolean useTls = false;
    public String username = "";
    public String password = "";
    public int panelWidth = 320;
    public int panelHeight = 200;
    public String layout = "BOTTOM_RIGHT";
    public int opacity = 80;
    public boolean autoConnect = true;
    public boolean echoToGameChat = true;
    public String nameColor = "";
    public String icon = "";

    public static ChatConfig get() {
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("friendchat.json");
    }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                ChatConfig c = GSON.fromJson(Files.readString(p), ChatConfig.class);
                if (c != null) instance = c;
            }
        } catch (Exception ignored) {
        }
        instance.clamp();
        save();
    }

    public static void save() {
        try {
            instance.clamp();
            Files.writeString(path(), GSON.toJson(instance));
        } catch (Exception ignored) {
        }
    }

    public void clamp() {
        if (serverHost == null || serverHost.isBlank()) serverHost = "localhost";
        if (username == null) username = "";
        if (password == null) password = "";
        serverPort = Math.max(1, Math.min(65535, serverPort));
        panelWidth = Math.max(160, Math.min(900, panelWidth));
        panelHeight = Math.max(110, Math.min(700, panelHeight));
        opacity = Math.max(10, Math.min(100, opacity));
        if (nameColor == null || !nameColor.matches("#[0-9a-fA-F]{6}")) nameColor = "";
        icon = icon == null ? "" : ChatStyle.firstChars(icon.trim(), 2);
        boolean ok = false;
        for (String l : LAYOUTS) if (l.equals(layout)) ok = true;
        if (!ok) layout = "BOTTOM_RIGHT";
    }

    public String effectiveName() {
        if (!username.isBlank()) return username.trim();
        try {
            return Minecraft.getInstance().getUser().getName();
        } catch (Throwable t) {
            return "Player";
        }
    }
}
