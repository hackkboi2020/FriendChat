package dev.friendchat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

final class Gfx {
    private Gfx() {
    }

    static void rrect(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int c) {
        g.fill(x1 + 1, y1, x2 - 1, y1 + 1, c);
        g.fill(x1, y1 + 1, x2, y2 - 1, c);
        g.fill(x1 + 1, y2 - 1, x2 - 1, y2, c);
    }

    static void avatar(GuiGraphicsExtractor g, Font f, int x, int y, int rgb, String name) {
        rrect(g, x, y, x + 18, y + 18, rgb);
        String s = (name == null || name.isEmpty()) ? "?" : name.substring(0, 1).toUpperCase();
        int tc = ChatStyle.lum(rgb) > 150 ? 0xFF1E1F22 : 0xFFFFFFFF;
        g.text(f, s, x + 9 - f.width(s) / 2, y + 5, tc, false);
    }
}
