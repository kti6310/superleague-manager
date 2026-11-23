package superleague.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.AbstractButton;

final class Widgets {
    private Widgets() {
    }

    static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    static void drawCentered(Graphics2D g2, String text, int w, int h) {
        FontMetrics fm = g2.getFontMetrics();
        int x = (w - fm.stringWidth(text)) / 2;
        int y = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, x, y);
    }

    static void focusRing(Graphics2D g2, JComponent c, int arc) {
        if (c.isFocusOwner()) {
            g2.setColor(Theme.FOCUS);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1, c.getWidth() - 3, c.getHeight() - 3, arc, arc);
        }
    }

    static class RoundPanel extends JPanel {
        private Color line = Theme.BORDER;
        private int lineWidth = 1;
        private final int arc;
        // colour fade when hover/selection changes (from → current target)
        private Color bgFrom;
        private Color lineFrom;
        private Anim.Value fade;

        RoundPanel(LayoutManager layout, int arc) {
            super(layout);
            this.arc = arc;
            setOpaque(false);
            setBackground(Theme.CARD);
            fade = new Anim.Value(this, 1, 160);
        }

        void restyle(java.util.Map<Integer, Color> map) {
            line = Theme.lineColor(line, map);
        }

        private Color shownBg() {
            return fade == null ? getBackground() : Anim.mix(bgFrom, getBackground(), fade.get());
        }

        private Color shownLine() {
            return fade == null ? line : Anim.mix(lineFrom, line, fade.get());
        }

        private void beginFade() {
            if (fade != null && isShowing()) {
                bgFrom = shownBg();
                lineFrom = shownLine();
                fade.snap(0);
                fade.animateTo(1);
            }
        }

        @Override
        public void setBackground(Color bg) {
            if (fade != null && bg != null && !bg.equals(getBackground())) {
                beginFade();
            }
            super.setBackground(bg);
        }

        void setLine(Color line, int width) {
            if (line != null && !line.equals(this.line)) {
                beginFade();
            }
            this.line = line;
            this.lineWidth = width;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setColor(shownBg());
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            if (line != null) {
                g2.setColor(shownLine());
                g2.setStroke(new BasicStroke(lineWidth));
                float o = lineWidth / 2f;
                g2.drawRoundRect(Math.round(o), Math.round(o), Math.round(getWidth() - 1 - lineWidth),
                        Math.round(getHeight() - 1 - lineWidth), arc, arc);
            }
            g2.dispose();
        }
    }

    enum Kind {
        PRIMARY, SECONDARY, GHOST
    }

    static class FlatButton extends JButton {
        private final Kind kind;
        private final Anim.Value hoverAnim = new Anim.Value(this, 0, 150);
        private final Anim.Value pressAnim = new Anim.Value(this, 0, 90);
        private Dimension fixedSize;

        /** Same-size buttons (e.g. title screen); other buttons keep their text-based width. */
        void fixSize(int w, int h) {
            fixedSize = new Dimension(w, h);
            setMaximumSize(fixedSize);
            revalidate();
        }

        FlatButton(String text, Kind kind) {
            super(text);
            this.kind = kind;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setFont(Theme.font(Font.BOLD, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
            getModel().addChangeListener(e -> {
                boolean on = isEnabled();
                hoverAnim.animateTo(on && getModel().isRollover() ? 1 : 0);
                pressAnim.animateTo(on && getModel().isPressed() ? 1 : 0);
            });
        }

        @Override
        public Dimension getPreferredSize() {
            if (fixedSize != null) {
                return fixedSize;
            }
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(getText()) + 36, 42);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth();
            int h = getHeight();
            boolean on = isEnabled();
            double hover = on ? hoverAnim.get() : 0;
            double down = on ? pressAnim.get() : 0;
            Color bg;
            Color fg;
            Color line = null;
            switch (kind) {
                case PRIMARY -> {
                    bg = !on ? Theme.DISABLED_BG
                            : Anim.mix(Anim.mix(Theme.ACCENT, Theme.ACCENT_HOVER, hover), Theme.ACCENT_PRESS, down);
                    fg = on ? Color.WHITE : Theme.DISABLED_TEXT;
                }
                case SECONDARY -> {
                    bg = !on ? Theme.CARD
                            : Anim.mix(Anim.mix(Theme.CARD, Theme.SUBTLE, hover), Theme.SUBTLE_PRESS, down);
                    fg = on ? Theme.TEXT : Theme.DISABLED_TEXT;
                    line = on ? Anim.mix(Theme.BORDER_STRONG, Theme.ACCENT_BRIGHT, hover * 0.6) : Theme.BORDER_STRONG;
                }
                default -> {
                    bg = Anim.mix(Anim.mix(new Color(0, 0, 0, 0), Theme.SUBTLE, hover), Theme.SUBTLE_PRESS, down);
                    fg = on ? Theme.ACCENT : Theme.DISABLED_TEXT;
                }
            }
            // pressed buttons sink 1px so the click feels physical
            int sink = (int) Math.round(down);
            g2.translate(0, sink);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, w - 1, h - 1 - sink, 10, 10);
            if (line != null) {
                g2.setColor(line);
                g2.drawRoundRect(0, 0, w - 1, h - 1 - sink, 10, 10);
            }
            focusRing(g2, this, 10);
            g2.setFont(getFont());
            g2.setColor(fg);
            drawCentered(g2, getText(), w, h);
            g2.dispose();
        }
    }

    static class Segmented extends JPanel {
        private final JToggleButton[] items;
        private boolean silent;
        private final Anim.Value pill = new Anim.Value(this, -1, 220);

        Segmented(String[] labels, IntConsumer onSelect) {
            super(new GridLayout(1, labels.length, 2, 0));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
            items = new JToggleButton[labels.length];
            ButtonGroup group = new ButtonGroup();
            for (int i = 0; i < labels.length; i++) {
                int index = i;
                JToggleButton b = new SegmentButton(labels[i]);
                b.addActionListener(e -> {
                    if (!silent) {
                        onSelect.accept(index);
                    }
                });
                b.addItemListener(e -> {
                    if (b.isSelected()) {
                        if (pill.get() < 0) {
                            pill.snap(index);
                        } else {
                            pill.animateTo(index);
                        }
                    }
                });
                group.add(b);
                items[i] = b;
                add(b);
            }
        }

        void setSelectedIndex(int index) {
            silent = true;
            items[index].setSelected(true);
            silent = false;
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            for (JToggleButton b : items) {
                b.setEnabled(enabled);
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(60, items != null && items.length > 3 ? 44 : 40);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setColor(Theme.TRACK);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            double p = pill.get();
            if (p >= 0 && items.length > 0) {
                int lo = (int) Math.floor(p);
                int hi = Math.min(items.length - 1, lo + 1);
                double f = p - lo;
                java.awt.Rectangle a = items[Math.max(0, lo)].getBounds();
                java.awt.Rectangle b = items[hi].getBounds();
                int x = (int) Math.round(a.x + (b.x - a.x) * f);
                int w = (int) Math.round(a.width + (b.width - a.width) * f);
                // the white "selected" pill slides between options instead of jumping
                g2.setColor(new Color(15, 27, 51, 18));
                g2.fillRoundRect(x, a.y + 1, w - 1, a.height - 1, 8, 8);
                g2.setColor(isEnabled() ? Theme.CARD : Theme.SUBTLE);
                g2.fillRoundRect(x, a.y, w - 1, a.height - 2, 8, 8);
            }
            g2.dispose();
        }
    }

    private static class SegmentButton extends JToggleButton {
        SegmentButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth();
            int h = getHeight();
            boolean on = isEnabled();
            if (!isSelected() && on && getModel().isRollover()) {
                g2.setColor(Theme.TRACK_HOVER);
                g2.fillRoundRect(0, 0, w - 1, h - 2, 8, 8);
            }
            focusRing(g2, this, 8);
            g2.setFont(Theme.font(isSelected() ? Font.BOLD : Font.PLAIN, 13));
            g2.setColor(!on ? Theme.DISABLED_TEXT : isSelected() ? Theme.ACCENT : Theme.MUTED);
            String text = getText();
            FontMetrics fm = g2.getFontMetrics();
            if (fm.stringWidth(text) > w - 8 && text.contains(" ")) {
                g2.setFont(Theme.font(isSelected() ? Font.BOLD : Font.PLAIN, 12));
                fm = g2.getFontMetrics();
                int cut = text.indexOf(' ');
                String a = text.substring(0, cut);
                String b = text.substring(cut + 1);
                int lh = fm.getHeight() - 2;
                int top = (h - 1 - lh * 2) / 2 + fm.getAscent();
                g2.drawString(a, (w - fm.stringWidth(a)) / 2, top);
                g2.drawString(b, (w - fm.stringWidth(b)) / 2, top + lh);
            } else {
                if (fm.stringWidth(text) > w - 8) {
                    g2.setFont(Theme.font(isSelected() ? Font.BOLD : Font.PLAIN, 12));
                }
                drawCentered(g2, text, w, h - 1);
            }
            g2.dispose();
        }
    }

    static class Chip extends JComponent {
        private String text;
        private Color color;

        Chip(String text, Color color) {
            this.text = text;
            this.color = color;
            setFont(Theme.font(Font.BOLD, 11));
        }

        void set(String text, Color color) {
            this.text = text;
            this.color = color;
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(text) + 16, 20);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setColor(color);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g2.setColor(Color.WHITE);
            g2.setFont(getFont());
            drawCentered(g2, text, getWidth(), getHeight());
            g2.dispose();
        }
    }

    static class SeasonProgress extends JComponent {
        private final Anim.Value fraction = new Anim.Value(this, 0, 500);

        void setFraction(double fraction) {
            this.fraction.animateTo(Math.max(0, Math.min(1, fraction)));
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(100, 4);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setColor(Theme.NAVY_LIGHT);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Theme.ACCENT_BRIGHT);
            g2.fillRect(0, 0, (int) Math.round(getWidth() * fraction.get()), getHeight());
            g2.dispose();
        }
    }

    static class CheckIcon implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            AbstractButton b = (AbstractButton) c;
            Graphics2D g2 = smooth(g);
            boolean on = b.isSelected();
            boolean enabled = b.isEnabled();
            g2.setColor(on ? (enabled ? Theme.ACCENT : Theme.DISABLED_BG) : Theme.CARD);
            g2.fillRoundRect(x, y, 17, 17, 5, 5);
            if (!on) {
                g2.setColor(enabled ? Theme.BORDER_STRONG : Theme.BORDER);
                g2.drawRoundRect(x, y, 16, 16, 5, 5);
            } else {
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawPolyline(new int[]{x + 4, x + 7, x + 13}, new int[]{y + 9, y + 12, y + 5}, 3);
            }
            if (b.isFocusOwner()) {
                g2.setColor(Theme.FOCUS);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(x - 2, y - 2, 21, 21, 7, 7);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 18;
        }

        @Override
        public int getIconHeight() {
            return 18;
        }
    }
}
