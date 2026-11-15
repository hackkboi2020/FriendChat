package dev.friendchat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FriendChatScreen extends Screen {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private record Row(int group, boolean head, boolean system, String name, int color, String icon, String time, String text, int h) {
    }

    private final Screen parent;
    private EditBox input;
    private String draft = "";
    private int px, py, pw, ph, scrollPx;
    private List<Row> rows = new ArrayList<>();
    private int totalH, cacheVersion = -1, cacheW = -1;

    public FriendChatScreen(Screen parent) {
        super(Component.literal("Friend Chat"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (input != null) draft = input.getValue();
        ChatConfig c = ChatConfig.get();
        pw = Math.max(180, Math.min(c.panelWidth, width - 8));
        ph = Math.max(120, Math.min(c.panelHeight, height - 8));
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
        cacheVersion = -1;
        input = new EditBox(font, px + 12, py + ph - 21, pw - 12 - 6 - 24, 12, Component.literal("Message"));
        input.setBordered(false);
        input.setMaxLength(256);
        input.setHint(Component.literal("Message everyone..."));
        input.setValue(draft);
        addRenderableWidget(input);
        setInitialFocus(input);
        addRenderableWidget(Button.builder(Component.literal("\u27A4"), b -> send())
                .bounds(px + pw - 6 - 20, py + ph - 23, 18, 16).build());
        int by = py + 5;
        addRenderableWidget(Button.builder(Component.literal("\u2699"), b -> minecraft.setScreenAndShow(new FriendConfigScreen(this)))
                .bounds(px + pw - 6 - 16, by, 16, 14).build());
        addRenderableWidget(Button.builder(Component.literal("\u21BB"), b -> ChatClient.INSTANCE.connect())
                .bounds(px + pw - 6 - 16 - 3 - 16, by, 16, 14).build());
        addRenderableWidget(Button.builder(Component.literal("\u2726"), b -> minecraft.setScreenAndShow(new FriendStyleScreen(this)))
                .bounds(px + pw - 6 - 16 - 3 - 16 - 3 - 16, by, 16, 14).build());
        addRenderableWidget(Button.builder(Component.literal(redirectLabel()), b -> {
            FriendChatClient.toggleRedirect();
            b.setMessage(Component.literal(redirectLabel()));
        }).bounds(px + pw - 6 - 16 * 4 - 9, by, 16, 14).build());
    }

    private static String redirectLabel() {
        return ChatConfig.get().redirectGameChat ? "\u00a7a\u21C4" : "\u21C4";
    }

    private void send() {
        String t = input.getValue().trim();
        if (t.isEmpty()) return;
        if (!ChatClient.INSTANCE.send(t)) ChatClient.INSTANCE.addSystem("Not connected to the chat server.");
        input.setValue("");
        scrollPx = 0;
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
        scrollPx += (int) Math.signum(sy) * 24;
        if (scrollPx < 0) scrollPx = 0;
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(parent);
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

    private void addRow(Row r) {
        rows.add(r);
        totalH += r.h();
    }

    private void layout() {
        int bodyW = pw - 8 - 24 - 8;
        int sysW = pw - 16;
        int ver = ChatClient.INSTANCE.version();
        if (ver == cacheVersion && bodyW == cacheW) return;
        cacheVersion = ver;
        cacheW = bodyW;
        rows = new ArrayList<>();
        totalH = 0;
        int lh = font.lineHeight + 2;
        ChatMessage prev = null;
        int group = -1;
        for (ChatMessage m : ChatClient.INSTANCE.snapshot(150)) {
            String time = TIME.format(Instant.ofEpochMilli(m.time()));
            if (m.system()) {
                group++;
                for (String s : wrap(font, "* " + m.text(), sysW, sysW)) addRow(new Row(group, false, true, "", 0xFFF0B232, "", time, s, lh));
                prev = m;
                continue;
            }
            boolean head = prev == null || prev.system() || !prev.user().equals(m.user()) || m.time() - prev.time() > 300000L;
            int color = ChatStyle.rgb(m.color(), m.user());
            if (head) {
                group++;
                addRow(new Row(group, true, false, m.user(), color, m.icon(), time, "", 14));
            }
            for (String s : wrap(font, m.text(), bodyW, bodyW)) addRow(new Row(group, false, false, m.user(), color, m.icon(), time, s, lh));
            prev = m;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractBackground(g, mx, my, pt);
        ChatConfig c = ChatConfig.get();
        int a = Math.max(0, Math.min(255, (int) (c.opacity * 2.55f)));
        int a2 = Math.min(255, a + 25);
        Gfx.rrect(g, px, py, px + pw, py + ph, (a << 24) | 0x313338);
        Gfx.rrect(g, px, py, px + pw, py + 24, (a2 << 24) | 0x2B2D31);
        g.fill(px, py + 24, px + pw, py + 25, 0xFF1E1F22);
        g.text(font, "Friend Chat", px + 8, py + 8, 0xFFFFFFFF, false);
        String st = ChatClient.INSTANCE.status();
        int stColor = st.equals("Connected") ? 0xFF23A55A : st.startsWith("Connecting") ? 0xFFF0B232 : 0xFFF23F43;
        int dx = px + 8 + font.width("Friend Chat") + 8;
        g.fill(dx, py + 9, dx + 6, py + 15, stColor);
        if (!st.equals("Connected")) g.text(font, st, dx + 10, py + 8, stColor, false);

        layout();
        int top = py + 28, bottom = py + ph - 30, area = bottom - top;
        scrollPx = Math.max(0, Math.min(scrollPx, Math.max(0, totalH - area)));
        int y0 = bottom - totalH + scrollPx;
        int hov = -1;
        if (mx >= px && mx < px + pw && my >= top && my < bottom) {
            int yy = y0;
            for (Row r : rows) {
                if (my >= yy && my < yy + r.h()) {
                    hov = r.group();
                    break;
                }
                yy += r.h();
                if (yy > my) break;
            }
        }
        int yy = y0;
        int tx = px + 8 + 24;
        for (Row r : rows) {
            int h = r.h();
            if (yy + h <= top) {
                yy += h;
                continue;
            }
            if (yy >= bottom) break;
            if (yy >= top && yy + h <= bottom) {
                if (r.group() == hov) g.fill(px + 3, yy, px + pw - 3, yy + h, 0x14FFFFFF);
                if (r.system()) {
                    g.text(font, r.text(), px + 8, yy + 1, r.color(), false);
                } else if (r.head()) {
                    Gfx.avatar(g, font, px + 8, yy + 3, r.color(), r.name());
                    g.text(font, r.name(), tx, yy + 3, r.color(), false);
                    int x = tx + font.width(r.name()) + 4;
                    if (!r.icon().isEmpty()) {
                        g.text(font, r.icon(), x, yy + 3, r.color(), false);
                        x += font.width(r.icon()) + 4;
                    }
                    g.text(font, r.time(), x, yy + 3, 0xFF949BA4, false);
                } else {
                    g.text(font, r.text(), tx, yy + 1, 0xFFDBDEE1, false);
                }
            }
            yy += h;
        }
        if (scrollPx > 0) {
            Gfx.rrect(g, px + pw - 30, bottom - 14, px + pw - 8, bottom - 2, 0xFF5865F2);
            g.text(font, "\u2193", px + pw - 22, bottom - 12, 0xFFFFFFFF, false);
        }
        Gfx.rrect(g, px + 6, py + ph - 25, px + pw - 6, py + ph - 5, 0xFF383A40);
    }
}
