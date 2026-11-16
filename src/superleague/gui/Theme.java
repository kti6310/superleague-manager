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
    static final Color BG = new Color(0xF3F5F9);
    static final Color CARD = Color.WHITE;
    static final Color SUBTLE = new Color(0xF1F4F9);
    static final Color SUBTLE_PRESS = new Color(0xE4E9F2);
    static final Color TRACK = new Color(0xEBEFF5);
    static final Color TRACK_HOVER = new Color(0xDFE5EE);
    static final Color NAVY = new Color(0x0F1B33);
    static final Color NAVY_LIGHT = new Color(0x1C2B4A);
    static final Color ACCENT = new Color(0x2563EB);
    static final Color ACCENT_HOVER = new Color(0x1D55D4);
    static final Color ACCENT_PRESS = new Color(0x1846B0);
    static final Color ACCENT_BRIGHT = new Color(0x60A5FA);
    static final Color ACCENT_SOFT = new Color(0xEAF1FE);
    static final Color FOCUS = new Color(37, 99, 235, 110);
    static final Color GOOD = new Color(0x15803D);
    static final Color WARN = new Color(0xD97706);
    static final Color BAD = new Color(0xDC2626);
    static final Color TEXT = new Color(0x111827);
    static final Color MUTED = new Color(0x5B6475);
    static final Color BORDER = new Color(0xE3E7EF);
    static final Color BORDER_STRONG = new Color(0xCBD2DE);
    static final Color ZEBRA = new Color(0xF9FAFC);
    static final Color DISABLED_BG = new Color(0xC9D3E3);
    static final Color DISABLED_TEXT = new Color(0x9AA3B2);
    static final Color SCROLL_THUMB = new Color(0xCBD2DE);
    static final Color SCROLL_THUMB_HOVER = new Color(0xA9B3C4);
    static final Color ON_DARK_MUTED = new Color(0xA9B4CC);

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
        UIManager.put("Panel.background", new ColorUIResource(BG));
        UIManager.put("OptionPane.background", new ColorUIResource(CARD));
        UIManager.put("OptionPane.messageForeground", new ColorUIResource(TEXT));
        UIManager.put("ToolTip.background", new ColorUIResource(NAVY));
        UIManager.put("ToolTip.foreground", new ColorUIResource(Color.WHITE));
        UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(6, 8, 6, 8));
        UIManager.put("ComboBox.selectionBackground", new ColorUIResource(ACCENT_SOFT));
        UIManager.put("ComboBox.selectionForeground", new ColorUIResource(TEXT));
        UIManager.put("List.selectionBackground", new ColorUIResource(ACCENT_SOFT));
        UIManager.put("List.selectionForeground", new ColorUIResource(TEXT));
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(BORDER_STRONG));
        UIManager.put("CheckBox.icon", new Widgets.CheckIcon());
        UIManager.put("TabbedPaneUI", FlatUI.Tabs.class.getName());
        UIManager.put("SliderUI", FlatUI.Slider.class.getName());
        UIManager.put("ScrollBarUI", FlatUI.ScrollBar.class.getName());
        UIManager.put("ComboBoxUI", FlatUI.Combo.class.getName());
        UIManager.put("ProgressBarUI", FlatUI.Progress.class.getName());
        UIManager.put("Button.background", new ColorUIResource(CARD));
        UIManager.put("Button.select", new ColorUIResource(SUBTLE_PRESS));
        UIManager.put("Button.focus", new ColorUIResource(FOCUS));
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
