package superleague.ui;

public final class Text {
    private Text() {
    }

    public static int displayWidth(String s) {
        int width = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            width += isWide(c) ? 2 : 1;
        }
        return width;
    }

    public static String padRight(String s, int width) {
        int pad = width - displayWidth(s);
        return pad > 0 ? s + " ".repeat(pad) : s;
    }

    public static String bar(int value, int min, int max, int length) {
        int filled = (int) Math.round((double) (value - min) / (max - min) * length);
        filled = Math.max(0, Math.min(length, filled));
        return "[" + "#".repeat(filled) + "-".repeat(length - filled) + "]";
    }

    private static boolean isWide(char c) {
        return (c >= 0x1100 && c <= 0x115F)
                || (c >= 0x2E80 && c <= 0xA4CF)
                || (c >= 0xAC00 && c <= 0xD7A3)
                || (c >= 0xF900 && c <= 0xFAFF)
                || (c >= 0xFF00 && c <= 0xFF60);
    }
}
