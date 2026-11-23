package superleague.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

final class Theme {
    static Color BG;
    static Color CARD;
    static Color SUBTLE;
    static Color SUBTLE_PRESS;
    static Color TRACK;
    static Color TRACK_HOVER;
    static final Color NAVY = new Color(0x0F1B33);
    static final Color NAVY_LIGHT = new Color(0x1C2B4A);
    static final Color ACCENT = new Color(0x2563EB);
    static final Color ACCENT_HOVER = new Color(0x1D55D4);
    static final Color ACCENT_PRESS = new Color(0x1846B0);
    static final Color ACCENT_BRIGHT = new Color(0x60A5FA);
    static Color ACCENT_SOFT;
    static final Color FOCUS = new Color(37, 99, 235, 110);
    static Color GOOD;
    static Color WARN;
    static Color BAD;
    static Color TEXT;
    static Color MUTED;
    static Color BORDER;
    static Color BORDER_STRONG;
    static Color ZEBRA;
    static Color ROW_ALT;
    static Color DISABLED_BG;
    static Color DISABLED_TEXT;
    static Color SCROLL_THUMB;
    static Color SCROLL_THUMB_HOVER;
    static final Color ON_DARK_MUTED = new Color(0xA9B4CC);

    private static final int[] LIGHT = {0xF3F5F9, 0xFFFFFF, 0xF1F4F9, 0xE4E9F2, 0xEBEFF5, 0xDFE5EE, 0xEAF1FE, 0x15803D,
            0xD97706, 0xDC2626, 0x111827, 0x5B6475, 0xE3E7EF, 0xCBD2DE, 0xF9FAFC, 0xF8FAFC, 0xC9D3E3, 0x9AA3B2,
            0xCBD2DE, 0xA9B3C4};
    private static final int[] DARK = {0x0E131B, 0x171E2B, 0x1F2838, 0x2A3548, 0x263042, 0x303C52, 0x1C2C4F, 0x22C55E,
            0xF59E0B, 0xF87171, 0xE6EAF2, 0x9AA6BA, 0x2A3446, 0x3A475E, 0x1A2231, 0x1B2333, 0x2C3647, 0x6B778C,
            0x3A475E, 0x4B5A74};
    private static final int[] EXTRA_LIGHT_BG = {0xFAFAFC, 0xEEF1F5, 0xE3F1E6, 0xE3EAF6, 0xF1F5F9, 0xFEF3C7};
    private static final int[] EXTRA_DARK_BG = {0x171E2B, 0x2A3446, 0x1B3324, 0x1C2A44, 0x1F2838, 0x3A3218};
    private static boolean dark;

    static {
        boolean saved = false;
        try {
            saved = java.util.prefs.Preferences.userNodeForPackage(Theme.class).getBoolean("dark", false);
        } catch (Exception ignored) {
        }
        palette(saved);
    }

    private static void palette(boolean useDark) {
        dark = useDark;
        int[] p = useDark ? DARK : LIGHT;
        BG = new Color(p[0]);
        CARD = new Color(p[1]);
        SUBTLE = new Color(p[2]);
        SUBTLE_PRESS = new Color(p[3]);
        TRACK = new Color(p[4]);
        TRACK_HOVER = new Color(p[5]);
        ACCENT_SOFT = new Color(p[6]);
        GOOD = new Color(p[7]);
        WARN = new Color(p[8]);
        BAD = new Color(p[9]);
        TEXT = new Color(p[10]);
        MUTED = new Color(p[11]);
        BORDER = new Color(p[12]);
        BORDER_STRONG = new Color(p[13]);
        ZEBRA = new Color(p[14]);
        ROW_ALT = new Color(p[15]);
        DISABLED_BG = new Color(p[16]);
        DISABLED_TEXT = new Color(p[17]);
        SCROLL_THUMB = new Color(p[18]);
        SCROLL_THUMB_HOVER = new Color(p[19]);
    }

    static boolean isDark() {
        return dark;
    }

    /** Highlight colour for rows/boxes created after a window opened (those are not re-themed automatically). */
    static Color tint(int lightRgb, int darkRgb) {
        return new Color(dark ? darkRgb : lightRgb);
    }

    static void setDark(boolean useDark, Component root) {
        if (useDark == dark) {
            return;
        }
        java.util.Map<Integer, Color> map = colorMap(useDark);
        palette(useDark);
        try {
            java.util.prefs.Preferences.userNodeForPackage(Theme.class).putBoolean("dark", useDark);
        } catch (Exception ignored) {
        }
        installColors();
        if (root != null) {
            restyle(root, map);
            root.repaint();
        }
    }

    static void adapt(Component c) {
        if (dark && c != null) {
            restyle(c, colorMap(true));
        }
    }

    private static java.util.Map<Integer, Color> colorMap(boolean useDark) {
        java.util.Map<Integer, Color> map = new java.util.HashMap<>();
        int[] from = useDark ? LIGHT : DARK;
        int[] to = useDark ? DARK : LIGHT;
        for (int i = 0; i < from.length; i++) {
            map.putIfAbsent(from[i] & 0xFFFFFF, new Color(to[i]));
        }
        if (useDark) {
            for (int i = 0; i < EXTRA_LIGHT_BG.length; i++) {
                map.putIfAbsent(EXTRA_LIGHT_BG[i], new Color(EXTRA_DARK_BG[i]));
            }
        }
        return map;
    }

    private static Color swap(Color c, java.util.Map<Integer, Color> map, boolean foreground) {
        if (c == null || c.getAlpha() < 255 || (foreground && (c.getRGB() & 0xFFFFFF) == 0xFFFFFF)) {
            return c;
        }
        return map.getOrDefault(c.getRGB() & 0xFFFFFF, c);
    }

    private static Border swap(Border b, java.util.Map<Integer, Color> map) {
        if (b instanceof javax.swing.border.CompoundBorder cb) {
            return BorderFactory.createCompoundBorder(swap(cb.getOutsideBorder(), map), swap(cb.getInsideBorder(), map));
        }
        if (b instanceof javax.swing.border.MatteBorder mb && mb.getMatteColor() != null) {
            java.awt.Insets in = mb.getBorderInsets();
            return BorderFactory.createMatteBorder(in.top, in.left, in.bottom, in.right, swap(mb.getMatteColor(), map, false));
        }
        if (b instanceof javax.swing.border.LineBorder lb) {
            return BorderFactory.createLineBorder(swap(lb.getLineColor(), map, false), lb.getThickness());
        }
        return b;
    }

    private static void restyle(Component c, java.util.Map<Integer, Color> map) {
        if (c.isBackgroundSet()) {
            c.setBackground(swap(c.getBackground(), map, false));
        }
        if (c.isForegroundSet()) {
            c.setForeground(swap(c.getForeground(), map, true));
        }
        if (c instanceof javax.swing.JComponent jc && jc.getBorder() != null) {
            jc.setBorder(swap(jc.getBorder(), map));
        }
        if (c instanceof Widgets.RoundPanel rp) {
            rp.restyle(map);
        }
        if (c instanceof javax.swing.JTable t) {
            t.setGridColor(swap(t.getGridColor(), map, false));
            t.setSelectionBackground(swap(t.getSelectionBackground(), map, false));
            t.setSelectionForeground(swap(t.getSelectionForeground(), map, true));
            if (t.getTableHeader() != null) {
                restyle(t.getTableHeader(), map);
            }
        }
        if (c instanceof javax.swing.JList<?> l) {
            l.setSelectionBackground(swap(l.getSelectionBackground(), map, false));
            l.setSelectionForeground(swap(l.getSelectionForeground(), map, true));
        }
        if (c instanceof java.awt.Container k) {
            for (Component child : k.getComponents()) {
                restyle(child, map);
            }
        }
    }

    static JButton themeToggle(Component owner) {
        JButton b = secondaryButton(dark ? "라이트 모드" : "다크 모드");
        b.addActionListener(e -> {
            setDark(!dark, javax.swing.SwingUtilities.getWindowAncestor(owner));
            b.setText(dark ? "라이트 모드" : "다크 모드");
        });
        return b;
    }

    static Color lineColor(Color line, java.util.Map<Integer, Color> map) {
        return swap(line, map, false);
    }

    private static final List<String> FONT_CANDIDATES = List.of(
            "Malgun Gothic", "맑은 고딕", "Apple SD Gothic Neo", "NanumGothic", "나눔고딕",
            "Noto Sans CJK KR", "Noto Sans KR", "Dialog");

    private static String family = "Dialog";

    private Theme() {
    }

    static void install() {
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String candidate : FONT_CANDIDATES) {
            if (available.contains(candidate)) {
                family = candidate;
                break;
            }
        }
        if (family.equals("Dialog")) {
            family = loadBrowserFont();
        }
        FontUIResource base = new FontUIResource(family, Font.PLAIN, 13);
        UIManager.getDefaults().keySet().forEach(key -> {
            if (UIManager.get(key) instanceof FontUIResource) {
                UIManager.put(key, base);
            }
        });
        installColors();
        java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(e -> {
            if (e.getID() == java.awt.event.WindowEvent.WINDOW_OPENED) {
                adapt((Component) e.getSource());
            }
        }, java.awt.AWTEvent.WINDOW_EVENT_MASK);
        UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(6, 8, 6, 8));
        UIManager.put("CheckBox.icon", new Widgets.CheckIcon());
        UIManager.put("TabbedPaneUI", FlatUI.Tabs.class.getName());
        UIManager.put("SliderUI", FlatUI.Slider.class.getName());
        UIManager.put("ScrollBarUI", FlatUI.ScrollBar.class.getName());
        UIManager.put("ComboBoxUI", FlatUI.Combo.class.getName());
        UIManager.put("ProgressBarUI", FlatUI.Progress.class.getName());
        UIManager.put("Button.focus", new ColorUIResource(FOCUS));
    }

    private static void installColors() {
        UIManager.put("Panel.background", new ColorUIResource(BG));
        UIManager.put("Label.foreground", new ColorUIResource(TEXT));
        UIManager.put("TextField.background", new ColorUIResource(CARD));
        UIManager.put("TextField.foreground", new ColorUIResource(TEXT));
        UIManager.put("TextField.caretForeground", new ColorUIResource(TEXT));
        UIManager.put("ComboBox.background", new ColorUIResource(CARD));
        UIManager.put("ComboBox.foreground", new ColorUIResource(TEXT));
        UIManager.put("List.background", new ColorUIResource(CARD));
        UIManager.put("List.foreground", new ColorUIResource(TEXT));
        UIManager.put("Table.background", new ColorUIResource(CARD));
        UIManager.put("Table.foreground", new ColorUIResource(TEXT));
        UIManager.put("TableHeader.background", new ColorUIResource(SUBTLE));
        UIManager.put("TableHeader.foreground", new ColorUIResource(MUTED));
        UIManager.put("Viewport.background", new ColorUIResource(CARD));
        UIManager.put("ScrollPane.background", new ColorUIResource(BG));
        UIManager.put("PopupMenu.background", new ColorUIResource(CARD));
        UIManager.put("MenuItem.background", new ColorUIResource(CARD));
        UIManager.put("MenuItem.foreground", new ColorUIResource(TEXT));
        UIManager.put("RadioButton.background", new ColorUIResource(CARD));
        UIManager.put("RadioButton.foreground", new ColorUIResource(TEXT));
        UIManager.put("CheckBox.background", new ColorUIResource(CARD));
        UIManager.put("CheckBox.foreground", new ColorUIResource(TEXT));
        UIManager.put("OptionPane.background", new ColorUIResource(CARD));
        UIManager.put("OptionPane.messageForeground", new ColorUIResource(TEXT));
        UIManager.put("ToolTip.background", new ColorUIResource(NAVY));
        UIManager.put("ToolTip.foreground", new ColorUIResource(Color.WHITE));
        UIManager.put("ComboBox.selectionBackground", new ColorUIResource(ACCENT_SOFT));
        UIManager.put("ComboBox.selectionForeground", new ColorUIResource(TEXT));
        UIManager.put("List.selectionBackground", new ColorUIResource(ACCENT_SOFT));
        UIManager.put("List.selectionForeground", new ColorUIResource(TEXT));
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(BORDER_STRONG));
        UIManager.put("Button.background", new ColorUIResource(CARD));
        UIManager.put("Button.select", new ColorUIResource(SUBTLE_PRESS));
    }

    private static String loadBrowserFont() {
        java.io.File f = new java.io.File("/app/web/font.ttf");
        if (!f.exists()) {
            return "Dialog";
        }
        try {
            Font k = Font.createFont(Font.TRUETYPE_FONT, f);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(k);
            return k.getFamily();
        } catch (Exception e) {
            return "Dialog";
        }
    }

    static Font font(int style, float size) {
        return new Font(family, style, Math.round(size));
    }

    static JLabel label(String text, int style, float size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(style, size));
        label.setForeground(color);
        return label;
    }

    static Border cardBorder() {
        return BorderFactory.createEmptyBorder(14, 16, 14, 16);
    }

    /** Text field look: grey outline, turns blue while typing so the active field is obvious. */
    static void styleInput(javax.swing.JTextField field) {
        java.util.function.Consumer<Boolean> apply = focused -> field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(focused ? ACCENT : BORDER_STRONG, focused ? 2 : 1),
                BorderFactory.createEmptyBorder(focused ? 7 : 8, focused ? 9 : 10, focused ? 7 : 8, focused ? 9 : 10)));
        apply.accept(false);
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                apply.accept(true);
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                apply.accept(false);
            }
        });
    }

    static JPanel card(String title, Component body) {
        Widgets.RoundPanel card = new Widgets.RoundPanel(new BorderLayout(0, 10), 14);
        card.setBorder(cardBorder());
        if (title != null) {
            card.add(label(title, Font.BOLD, 12, MUTED), BorderLayout.NORTH);
        }
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    static JButton primaryButton(String text) {
        return new Widgets.FlatButton(text, Widgets.Kind.PRIMARY);
    }

    static JButton secondaryButton(String text) {
        return new Widgets.FlatButton(text, Widgets.Kind.SECONDARY);
    }

    static Color positionColor(superleague.model.Position group) {
        return switch (group) {
            case FW -> new Color(0xDC2626);
            case MF -> new Color(0x16A34A);
            case DF -> new Color(0x2563EB);
            case GK -> new Color(0xEAB308);
        };
    }

    static Color positionColor(String positions) {
        superleague.model.Role r = superleague.model.Role.parse(positions == null ? "" : positions.split("[,\\s]+")[0]);
        return r == null ? TEXT : positionColor(r.group());
    }

    static Color onPositionColor(superleague.model.Position group) {
        return group == superleague.model.Position.GK ? new Color(0x1F2937) : Color.WHITE;
    }

    static Color tierColor(superleague.model.Tier tier) {
        return switch (tier) {
            case S -> new Color(0xB45309);
            case A -> new Color(0x2563EB);
            case B -> new Color(0x15803D);
            case C -> new Color(0x7C3AED);
        };
    }

    static String signed(double value) {
        return String.format("%+.1f%%", value * 100);
    }
}
