package superleague.gui;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

public final class FlatUI {
    private FlatUI() {
    }

    public static class Tabs extends BasicTabbedPaneUI {
        public static ComponentUI createUI(JComponent c) {
            return new Tabs();
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            tabInsets = new Insets(10, 16, 10, 16);
            selectedTabPadInsets = new Insets(0, 0, 0, 0);
            tabAreaInsets = new Insets(0, 4, 0, 4);
            contentBorderInsets = new Insets(1, 0, 0, 0);
            tabPane.setFont(Theme.font(Font.BOLD, 13));
            tabPane.setOpaque(false);
        }

        private Anim.Value barX;
        private Anim.Value barW;

        @Override
        protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
            super.paintTabArea(g, tabPlacement, selectedIndex);
            if (selectedIndex < 0 || rects == null || selectedIndex >= rects.length) {
                return;
            }
            Rectangle r = rects[selectedIndex];
            double tx = r.x + 8;
            double tw = Math.max(0, r.width - 16);
            if (barX == null) {
                barX = new Anim.Value(tabPane, tx, 240);
                barW = new Anim.Value(tabPane, tw, 240);
            } else {
                barX.animateTo(tx);
                barW.animateTo(tw);
            }
            // the blue underline glides to the newly selected tab
            Graphics2D g2 = Widgets.smooth(g);
            g2.setColor(Theme.ACCENT);
            g2.fillRoundRect((int) Math.round(barX.get()), r.y + r.height - 3,
                    (int) Math.round(barW.get()), 3, 3, 3);
            g2.dispose();
        }

        @Override
        protected void paintTabBackground(Graphics g, int placement, int index, int x, int y, int w, int h,
                                          boolean selected) {
            if (!selected && getRolloverTab() == index) {
                Graphics2D g2 = Widgets.smooth(g);
                g2.setColor(Theme.TRACK);
                g2.fillRoundRect(x + 2, y + 4, w - 4, h - 8, 8, 8);
                g2.dispose();
            }
        }

        @Override
        protected void paintTabBorder(Graphics g, int placement, int index, int x, int y, int w, int h,
                                      boolean selected) {
            // underline is drawn (animated) in paintTabArea
        }

        @Override
        protected void paintFocusIndicator(Graphics g, int placement, Rectangle[] rects, int index,
                                           Rectangle iconRect, Rectangle textRect, boolean selected) {
            if (tabPane.isFocusOwner() && selected) {
                Graphics2D g2 = Widgets.smooth(g);
                Rectangle r = rects[index];
                g2.setColor(Theme.FOCUS);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(r.x + 2, r.y + 3, r.width - 4, r.height - 7, 8, 8);
                g2.dispose();
            }
        }

        @Override
        protected void paintText(Graphics g, int placement, Font font, FontMetrics metrics, int index,
                                 String title, Rectangle textRect, boolean selected) {
            Graphics2D g2 = Widgets.smooth(g);
            g2.setFont(font);
            g2.setColor(selected ? Theme.TEXT : Theme.MUTED);
            g2.drawString(title, textRect.x, textRect.y + metrics.getAscent());
            g2.dispose();
        }

        @Override
        protected void paintContentBorder(Graphics g, int placement, int selectedIndex) {
            int y = calculateTabAreaHeight(placement, runCount, maxTabHeight);
            g.setColor(Theme.BORDER);
            g.fillRect(0, y - 1, tabPane.getWidth(), 1);
        }

        @Override
        protected int calculateTabHeight(int placement, int index, int fontHeight) {
            return fontHeight + 22;
        }
    }

    public static class Slider extends BasicSliderUI {
        public Slider(JSlider s) {
            super(s);
        }

        public static ComponentUI createUI(JComponent c) {
            return new Slider((JSlider) c);
        }

        @Override
        protected Dimension getThumbSize() {
            return new Dimension(18, 18);
        }

        @Override
        public void paintTrack(Graphics g) {
            Graphics2D g2 = Widgets.smooth(g);
            Rectangle t = trackRect;
            int cy = t.y + t.height / 2;
            g2.setColor(Theme.TRACK);
            g2.fillRoundRect(t.x, cy - 3, t.width, 6, 6, 6);
            int tx = thumbRect.x + thumbRect.width / 2;
            g2.setColor(slider.isEnabled() ? Theme.ACCENT : Theme.DISABLED_BG);
            g2.fillRoundRect(t.x, cy - 3, tx - t.x, 6, 6, 6);
            g2.dispose();
        }

        @Override
        public void paintThumb(Graphics g) {
            Graphics2D g2 = Widgets.smooth(g);
            Rectangle r = thumbRect;
            int cy = trackRect.y + trackRect.height / 2;
            int x = r.x + (r.width - 18) / 2;
            int y = cy - 9;
            g2.setColor(Theme.CARD);
            g2.fillOval(x, y, 18, 18);
            g2.setColor(slider.isEnabled() ? Theme.ACCENT : Theme.DISABLED_BG);
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawOval(x + 1, y + 1, 16, 16);
            if (slider.isFocusOwner()) {
                g2.setColor(Theme.FOCUS);
                g2.setStroke(new BasicStroke(4f));
                g2.drawOval(x - 2, y - 2, 22, 22);
            }
            g2.dispose();
        }

        @Override
        public void paintTicks(Graphics g) {
            Graphics2D g2 = Widgets.smooth(g);
            g2.setColor(Theme.BORDER_STRONG);
            int y = tickRect.y + 2;
            for (int v = slider.getMinimum(); v <= slider.getMaximum(); v += Math.max(1, slider.getMajorTickSpacing())) {
                int x = xPositionForValue(v);
                g2.fillOval(x - 2, y, 4, 4);
            }
            g2.dispose();
        }

        @Override
        public void paintFocus(Graphics g) {
        }
    }

    public static class ScrollBar extends BasicScrollBarUI {
        public static ComponentUI createUI(JComponent c) {
            return new ScrollBar();
        }

        @Override
        protected void configureScrollBarColors() {
            thumbColor = Theme.SCROLL_THUMB;
            trackColor = Theme.CARD;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return zero();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return zero();
        }

        private static JButton zero() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            return scrollbar.getOrientation() == javax.swing.JScrollBar.VERTICAL
                    ? new Dimension(10, 48) : new Dimension(48, 10);
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(c.getParent() != null ? c.getParent().getBackground() : Theme.CARD);
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = Widgets.smooth(g);
            g2.setColor(isThumbRollover() ? Theme.SCROLL_THUMB_HOVER : Theme.SCROLL_THUMB);
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6);
            g2.dispose();
        }
    }

    public static class Combo extends BasicComboBoxUI {
        public static ComponentUI createUI(JComponent c) {
            return new Combo();
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            comboBox.setBackground(Theme.CARD);
            comboBox.setForeground(Theme.TEXT);
            comboBox.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.BORDER_STRONG),
                    BorderFactory.createEmptyBorder(2, 6, 2, 2)));
        }

        @Override
        protected JButton createArrowButton() {
            JButton b = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = Widgets.smooth(g);
                    int cx = getWidth() / 2;
                    int cy = getHeight() / 2;
                    g2.setColor(comboBox.isEnabled() ? Theme.MUTED : Theme.DISABLED_TEXT);
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawPolyline(new int[]{cx - 4, cx, cx + 4}, new int[]{cy - 2, cy + 2, cy - 2}, 3);
                    g2.dispose();
                }
            };
            b.setBorder(BorderFactory.createEmptyBorder());
            b.setContentAreaFilled(false);
            b.setFocusPainted(false);
            b.setPreferredSize(new Dimension(24, 24));
            return b;
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            g.setColor(comboBox.isEnabled() ? Theme.CARD : Theme.SUBTLE);
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    public static class Progress extends BasicProgressBarUI {
        private Anim.Value fill;

        public static ComponentUI createUI(JComponent c) {
            return new Progress();
        }

        @Override
        protected void paintDeterminate(Graphics g, JComponent c) {
            JProgressBar bar = (JProgressBar) c;
            Graphics2D g2 = Widgets.smooth(g);
            int h = Math.min(8, c.getHeight());
            int y = (c.getHeight() - h) / 2;
            int w = c.getWidth();
            g2.setColor(Theme.TRACK);
            g2.fillRoundRect(0, y, w, h, h, h);
            double target = (bar.getValue() - bar.getMinimum()) / (double) Math.max(1, bar.getMaximum() - bar.getMinimum());
            if (fill == null) {
                fill = new Anim.Value(bar, target, 450);
            } else {
                fill.animateTo(target);
            }
            double frac = fill.get();
            int fw = (int) Math.round(w * frac);
            if (fw > 0) {
                g2.setColor(bar.getForeground());
                g2.fillRoundRect(0, y, Math.max(fw, h), h, h, h);
            }
            g2.dispose();
        }
    }
}
