package dev.friendchat;

final class ChatStyle {
    static final int[] PALETTE = {0xFF5DADE2, 0xFFF5B041, 0xFFEB984E, 0xFFAF7AC5, 0xFF48C9B0, 0xFFEC7063, 0xFFF4D03F, 0xFF85C1E9};

    private ChatStyle() {
    }

    static boolean valid(String s) {
        return s != null && s.matches("#[0-9a-fA-F]{6}");
    }

    static int rgb(String hex, String name) {
        if (valid(hex)) return 0xFF000000 | Integer.parseInt(hex.substring(1), 16);
        String n = name == null ? "" : name;
        return PALETTE[(n.hashCode() & 0x7FFFFFFF) % PALETTE.length];
    }

    static int lum(int rgb) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        return (r * 299 + g * 587 + b * 114) / 1000;
    }

    static String hex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    static String firstChars(String s, int n) {
        int[] cps = s.codePoints().limit(n).toArray();
        return new String(cps, 0, cps.length);
    }
}
