package dev.friendchat;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class FriendStyleScreen extends Screen {
    private static final int[] LEG = {0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA, 0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};
    private static final char[] CODES = {'1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final String[] ICONS = {
            "\u265B",
            "\u2620",
            "\u2694",
            "\u2702",
            "\u2605",
            "\u2665",
            "\u26A1",
            "\u262F",
            "\u265E",
            "\u273F",
            "\u2600",
            "\u2602",
            "\u2693",
            "\u2691",
            "\u266B",
            "\u2708",
            "\u2622",
            "\u2623",
            "\u272A",
            "\u2744",
            "\u2615",
            "\u2660",
            "\u2663",
            "\u2666"
    };

    private final Screen parent;
    private EditBox hex, icon;
    private String vHex, vIcon;
    private int y0 = 36, by;

    public FriendStyleScreen(Screen parent) {
        super(Component.literal("Customize"));
        this.parent = parent;
        vHex = ChatConfig.get().nameColor;
        vIcon = ChatConfig.get().icon;
    }

    private void capture() {
        if (hex == null) return;
        vHex = hex.getValue();
        vIcon = icon.getValue();
    }

    @Override
    protected void init() {
        capture();
        int cx = width / 2, lx = cx - 160, rx = cx + 10;
        for (int i = 0; i < LEG.length; i++) {
            final int idx = i;
            addRenderableWidget(Button.builder(Component.literal("\u00a7" + CODES[i] + "\u25A0"), b -> {
                hex.setValue(ChatStyle.hex(LEG[idx]));
            }).bounds(lx + (i % 8) * 19, y0 + 10 + (i / 8) * 19, 18, 18).build());
        }
        hex = new EditBox(font, lx, y0 + 64, 150, 18, Component.empty());
        hex.setMaxLength(7);
        hex.setValue(vHex == null ? "" : vHex);
        addRenderableWidget(hex);
        addRenderableWidget(Button.builder(Component.literal("Auto color"), b -> hex.setValue(""))
                .bounds(lx, y0 + 86, 150, 18).build());
        for (int i = 0; i < ICONS.length; i++) {
            final int idx = i;
            addRenderableWidget(Button.builder(Component.literal(ICONS[i]), b -> icon.setValue(ICONS[idx]))
                    .bounds(rx + (i % 8) * 20, y0 + 10 + (i / 8) * 20, 19, 19).build());
        }
        icon = new EditBox(font, rx, y0 + 82, 118, 18, Component.empty());
        icon.setMaxLength(4);
        icon.setValue(vIcon == null ? "" : vIcon);
        addRenderableWidget(icon);
        addRenderableWidget(Button.builder(Component.literal("None"), b -> icon.setValue(""))
                .bounds(rx + 122, y0 + 82, 38, 18).build());
        by = Math.min(height - 28, y0 + 156);
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(cx - 155, by, 150, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(cx + 5, by, 150, 20).build());
    }

    private void save() {
        capture();
        ChatConfig c = ChatConfig.get();
        String h = vHex == null ? "" : vHex.trim();
        c.nameColor = ChatStyle.valid(h) ? h.toUpperCase() : "";
        c.icon = ChatStyle.firstChars(vIcon == null ? "" : vIcon.trim(), 2);
        ChatConfig.save();
        minecraft.setScreenAndShow(parent);
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(parent);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractBackground(g, mx, my, pt);
        int cx = width / 2, lx = cx - 160, rx = cx + 10;
        Gfx.rrect(g, cx - 170, y0 - 28, cx + 170, by + 28, 0xE6313338);
        String title = "Customize your look";
        g.text(font, title, cx - font.width(title) / 2, y0 - 18, 0xFFFFFFFF, false);
        int lab = 0xFFB5BAC1;
        g.text(font, "Name color", lx, y0, lab, false);
        g.text(font, "Custom hex (#RRGGBB)", lx, y0 + 54, lab, false);
        g.text(font, "Icon next to your name", rx, y0, lab, false);
        g.text(font, "Custom icon (1-2 characters)", rx, y0 + 72, lab, false);
        if (hex != null && icon != null) {
            String name = ChatConfig.get().effectiveName();
            int color = ChatStyle.rgb(hex.getValue().trim(), name);
            int py = y0 + 112;
            Gfx.rrect(g, lx, py, lx + 320, py + 34, 0xFF2B2D31);
            Gfx.avatar(g, font, lx + 8, py + 8, color, name);
            g.text(font, name, lx + 32, py + 8, color, false);
            String ic = ChatStyle.firstChars(icon.getValue().trim(), 2);
            int x = lx + 32 + font.width(name) + 4;
            if (!ic.isEmpty()) {
                g.text(font, ic, x, py + 8, color, false);
                x += font.width(ic) + 4;
            }
            g.text(font, "now", x, py + 8, 0xFF949BA4, false);
            g.text(font, "This is how your messages will look", lx + 32, py + 19, 0xFFDBDEE1, false);
        }
    }
}
