package dev.friendchat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class FriendConfigScreen extends Screen {
    private final Screen parent;
    private EditBox host, port, name, pass, wid, hei;
    private String vHost, vPort, vName, vPass, vWid, vHei;
    private int layoutIdx, opacity;
    private boolean auto, echo, tls;

    public FriendConfigScreen(Screen parent) {
        super(Component.literal("Friend Chat Config"));
        this.parent = parent;
        ChatConfig c = ChatConfig.get();
        vHost = c.serverHost;
        vPort = String.valueOf(c.serverPort);
        vName = c.username;
        vPass = c.password;
        vWid = String.valueOf(c.panelWidth);
        vHei = String.valueOf(c.panelHeight);
        for (int i = 0; i < ChatConfig.LAYOUTS.length; i++) if (ChatConfig.LAYOUTS[i].equals(c.layout)) layoutIdx = i;
        opacity = c.opacity;
        auto = c.autoConnect;
        echo = c.echoToGameChat;
        tls = c.useTls;
    }

    private EditBox box(int x, int y, int w, String val, int max) {
        EditBox b = new EditBox(font, x, y, w, 18, Component.empty());
        b.setMaxLength(max);
        b.setValue(val);
        addRenderableWidget(b);
        return b;
    }

    private void capture() {
        if (host == null) return;
        vHost = host.getValue();
        vPort = port.getValue();
        vName = name.getValue();
        vPass = pass.getValue();
        vWid = wid.getValue();
        vHei = hei.getValue();
    }

    @Override
    protected void init() {
        capture();
        int lx = width / 2 - 155, rx = width / 2 + 5, w = 150, y0 = 44, dy = 30;
        host = box(lx, y0, w, vHost, 128);
        port = box(lx, y0 + dy, w, vPort, 5);
        name = box(lx, y0 + 2 * dy, w, vName, 32);
        pass = box(lx, y0 + 3 * dy, w, vPass, 64);
        wid = box(rx, y0, w, vWid, 4);
        hei = box(rx, y0 + dy, w, vHei, 4);
        addRenderableWidget(Button.builder(Component.literal("Layout: " + ChatConfig.LAYOUTS[layoutIdx]), b -> {
            layoutIdx = (layoutIdx + 1) % ChatConfig.LAYOUTS.length;
            b.setMessage(Component.literal("Layout: " + ChatConfig.LAYOUTS[layoutIdx]));
        }).bounds(rx, y0 + 2 * dy, w, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Opacity: " + opacity + "%"), b -> {
            opacity = opacity >= 100 ? 30 : opacity + 10;
            b.setMessage(Component.literal("Opacity: " + opacity + "%"));
        }).bounds(rx, y0 + 3 * dy, w, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Auto-connect: " + (auto ? "ON" : "OFF")), b -> {
            auto = !auto;
            b.setMessage(Component.literal("Auto-connect: " + (auto ? "ON" : "OFF")));
        }).bounds(lx, y0 + 4 * dy, w, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Show in game chat: " + (echo ? "ON" : "OFF")), b -> {
            echo = !echo;
            b.setMessage(Component.literal("Show in game chat: " + (echo ? "ON" : "OFF")));
        }).bounds(rx, y0 + 4 * dy, w, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Use TLS (wss): " + (tls ? "ON" : "OFF")), b -> {
            tls = !tls;
            b.setMessage(Component.literal("Use TLS (wss): " + (tls ? "ON" : "OFF")));
        }).bounds(lx, y0 + 5 * dy, w, 18).build());
        int by = Math.min(height - 28, y0 + 5 * dy + 26);
        addRenderableWidget(Button.builder(Component.literal("Save & Reconnect"), b -> save())
                .bounds(width / 2 - 155, by, 150, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(width / 2 + 5, by, 150, 20).build());
    }

    private static int num(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private void save() {
        capture();
        ChatConfig c = ChatConfig.get();
        c.serverHost = vHost.trim().replaceAll("^(wss?|https?)://", "").replaceAll("/.*$", "");
        c.useTls = tls;
        c.serverPort = num(vPort, c.serverPort);
        c.username = vName.trim();
        c.password = vPass;
        c.panelWidth = num(vWid, c.panelWidth);
        c.panelHeight = num(vHei, c.panelHeight);
        c.layout = ChatConfig.LAYOUTS[layoutIdx];
        c.opacity = opacity;
        c.autoConnect = auto;
        c.echoToGameChat = echo;
        ChatConfig.save();
        ChatClient.INSTANCE.connect();
        minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        String title = "Friend Chat Config";
        g.drawString(font, title, width / 2 - font.width(title) / 2, 16, 0xFFFFFFFF, false);
        int lx = width / 2 - 155, rx = width / 2 + 5, y0 = 44, dy = 30;
        int c = 0xFFA0A4AD;
        g.drawString(font, "Server host / IP", lx, y0 - 10, c, false);
        g.drawString(font, "Server port", lx, y0 + dy - 10, c, false);
        g.drawString(font, "Username (blank = MC name)", lx, y0 + 2 * dy - 10, c, false);
        g.drawString(font, "Password (if server has one)", lx, y0 + 3 * dy - 10, c, false);
        g.drawString(font, "Window width", rx, y0 - 10, c, false);
        g.drawString(font, "Window height", rx, y0 + dy - 10, c, false);
        g.drawString(font, "Window position", rx, y0 + 2 * dy - 10, c, false);
        g.drawString(font, "Background", rx, y0 + 3 * dy - 10, c, false);
    }
}
