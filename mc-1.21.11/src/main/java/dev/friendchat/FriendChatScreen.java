package dev.friendchat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FriendChatScreen extends Screen {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
    private static final int[] PALETTE = {0xFF5DADE2, 0xFFF5B041, 0xFFEB984E, 0xFFAF7AC5, 0xFF48C9B0, 0xFFEC7063, 0xFFF4D03F, 0xFF85C1E9};

    private record Line(String time, String name, int nameColor, String text, int textColor, boolean head) {
    }

    private final Screen parent;
    private EditBox input;
    private String draft = "";
    private int px, py, pw, ph, scroll;

    public FriendChatScreen(Screen parent) {
        super(Component.literal("Friend Chat"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (input != null) draft = input.getValue();
        ChatConfig c = ChatConfig.get();
        pw = Math.max(160, Math.min(c.panelWidth, width - 8));
        ph = Math.max(110, Math.min(c.panelHeight, height - 8));
        switch (c.layout) {
            case "CENTER" -> {
                px = (width - pw) / 2;
                py = (height - ph) / 2;
            }
            case "TOP_LEFT" -> {
                px = 4;
                py = 4;
            }
            case "TOP_RIGHT" -> {
                px = width - pw - 4;
                py = 4;
            }
            case "BOTTOM_LEFT" -> {
                px = 4;
                py = height - ph - 4;
            }
            default -> {
                px = width - pw - 4;
                py = height - ph - 4;
            }
        }
        input = new EditBox(font, px + 6, py + ph - 22, pw - 12 - 52, 16, Component.literal("Message"));
        input.setMaxLength(256);
        input.setHint(Component.literal("Type a message..."));
        input.setValue(draft);
        addRenderableWidget(input);
        setInitialFocus(input);
        addRenderableWidget(Button.builder(Component.literal("Send"), b -> send())
                .bounds(px + pw - 6 - 48, py + ph - 23, 48, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Config"), b -> minecraft.setScreen(new FriendConfigScreen(this)))
                .bounds(px + pw - 6 - 44, py + 4, 44, 14).build());
        addRenderableWidget(Button.builder(Component.literal("Reconnect"), b -> ChatClient.INSTANCE.connect())
                .bounds(px + pw - 6 - 44 - 4 - 62, py + 4, 62, 14).build());
    }

    private void send() {
        String t = input.getValue().trim();
        if (t.isEmpty()) return;
        if (!ChatClient.INSTANCE.send(t)) ChatClient.INSTANCE.addSystem("Not connected to the chat server.");
        input.setValue("");
        scroll = 0;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int k = event.key();
        if ((k == InputConstants.KEY_RETURN || k == InputConstants.KEY_NUMPADENTER) && input.isFocused()) {
            send();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        scroll += (int) Math.signum(sy) * 3;
        if (scroll < 0) scroll = 0;
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private static List<String> wrap(Font f, String s, int first, int rest) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int limit = first;
        for (String word : s.split(" ")) {
            String t = cur.length() == 0 ? word : cur + " " + word;
            if (f.width(t) <= limit) {
                cur.setLength(0);
                cur.append(t);
                continue;
            }
            if (cur.length() > 0) {
                out.add(cur.toString());
                cur.setLength(0);
                limit = rest;
            }
            String w = word;
            while (f.width(w) > limit && w.length() > 1) {
                int n = 1;
                while (n < w.length() && f.width(w.substring(0, n + 1)) <= limit) n++;
                out.add(w.substring(0, n));
                w = w.substring(n);
                limit = rest;
            }
            cur.append(w);
        }
        out.add(cur.toString());
        return out;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        ChatConfig c = ChatConfig.get();
        int a = Math.max(0, Math.min(255, (int) (c.opacity * 2.55f)));
        int a2 = Math.min(255, a + 30);
        g.fill(px, py, px + pw, py + ph, (a << 24) | 0x12141C);
        g.fill(px, py, px + pw, py + 18, (a2 << 24) | 0x1E2230);
        g.fill(px, py + 17, px + pw, py + 18, 0xFF5865F2);
        g.drawString(font, "Friend Chat", px + 6, py + 5, 0xFFFFFFFF, false);
        String st = ChatClient.INSTANCE.status();
        int stColor = st.equals("Connected") ? 0xFF57F287 : st.startsWith("Connecting") ? 0xFFF5B041 : 0xFFED4245;
        g.drawString(font, st, px + 6 + font.width("Friend Chat") + 8, py + 5, stColor, false);

        int top = py + 22;
        int bottom = py + ph - 28;
        int lineH = font.lineHeight + 1;
        int maxLines = Math.max(1, (bottom - top) / lineH);
        String self = c.effectiveName();
        List<Line> lines = new ArrayList<>();
        for (ChatMessage m : ChatClient.INSTANCE.snapshot(80)) {
            String time = TIME.format(java.time.Instant.ofEpochMilli(m.time()));
            int avail = pw - 12;
            int prefix = font.width("[" + time + "] ");
            if (m.system()) {
                List<String> parts = wrap(font, "* " + m.text(), avail - prefix, avail);
                for (int i = 0; i < parts.size(); i++) lines.add(new Line(time, null, 0, parts.get(i), 0xFFF5B041, i == 0));
            } else {
                int nameColor = m.user().equals(self) ? 0xFF57F287 : PALETTE[Math.abs(m.user().hashCode()) % PALETTE.length];
                prefix += font.width(m.user() + ": ");
                List<String> parts = wrap(font, m.text(), avail - prefix, avail);
                for (int i = 0; i < parts.size(); i++) lines.add(new Line(time, m.user(), nameColor, parts.get(i), 0xFFE6E6E6, i == 0));
            }
        }
        int total = lines.size();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, total - maxLines)));
        int end = total - scroll;
        int start = Math.max(0, end - maxLines);
        int y = bottom - (end - start) * lineH;
        for (int i = start; i < end; i++) {
            Line l = lines.get(i);
            int x = px + 6;
            if (l.head()) {
                String t = "[" + l.time() + "] ";
                g.drawString(font, t, x, y, 0xFF8A8F98, false);
                x += font.width(t);
                if (l.name() != null) {
                    String n = l.name() + ": ";
                    g.drawString(font, n, x, y, l.nameColor(), false);
                    x += font.width(n);
                }
            }
            g.drawString(font, l.text(), x, y, l.textColor(), false);
            y += lineH;
        }
        if (scroll > 0) g.drawString(font, "\u2193 " + scroll, px + pw - 40, bottom - 8, 0xFF8A8F98, false);
    }
}
