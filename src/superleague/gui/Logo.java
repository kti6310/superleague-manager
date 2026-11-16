package superleague.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

final class Logo extends JComponent {
    static final Color GOLD = new Color(0xF2C14E);
    private static final Color GOLD_DARK = new Color(0xB8862B);

    Logo() {
        setPreferredSize(new Dimension(520, 300));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Widgets.smooth(g);
        int w = getWidth();
        int crest = 150;
        paintCrest(g2, (w - crest) / 2, 0, crest);

        g2.setFont(Theme.font(Font.BOLD, 46));
        g2.setColor(Color.WHITE);
        drawCenteredAt(g2, "SUPER LEAGUE", w, crest + 62);
        g2.setColor(GOLD);
        g2.setFont(Theme.font(Font.BOLD, 20));
        drawCenteredAt(g2, spaced("MANAGER"), w, crest + 100);
        int lineY = crest + 93;
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(w / 2 - 190, lineY, w / 2 - 95, lineY);
        g2.drawLine(w / 2 + 95, lineY, w / 2 + 190, lineY);
        g2.dispose();
    }

    static void paintCrest(Graphics2D g2, int x, int y, int size) {
        double s = size / 100.0;
        Path2D shield = new Path2D.Double();
        shield.moveTo(x + 10 * s, y + 8 * s);
        shield.lineTo(x + 90 * s, y + 8 * s);
        shield.lineTo(x + 90 * s, y + 52 * s);
        shield.curveTo(x + 90 * s, y + 78 * s, x + 64 * s, y + 92 * s, x + 50 * s, y + 99 * s);
        shield.curveTo(x + 36 * s, y + 92 * s, x + 10 * s, y + 78 * s, x + 10 * s, y + 52 * s);
        shield.closePath();
        g2.setPaint(new GradientPaint(x, y, new Color(0x2563EB), x, y + size, new Color(0x13285C)));
        g2.fill(shield);
        g2.setColor(GOLD);
        g2.setStroke(new BasicStroke((float) (4 * s)));
        g2.draw(shield);

        int r = (int) (22 * s);
        int cx = x + (int) (50 * s);
        int cy = y + (int) (55 * s);
        g2.setColor(Color.WHITE);
        g2.fillOval(cx - r, cy - r, 2 * r, 2 * r);
        g2.setColor(Theme.NAVY);
        g2.fill(polygon(cx, cy, r * 0.42, -90, 5));
        g2.setStroke(new BasicStroke((float) (1.6 * s)));
        for (int i = 0; i < 5; i++) {
            double a = Math.toRadians(-90 + 72 * i);
            g2.drawLine((int) (cx + Math.cos(a) * r * 0.42), (int) (cy + Math.sin(a) * r * 0.42),
                    (int) (cx + Math.cos(a) * r), (int) (cy + Math.sin(a) * r));
        }
        g2.setColor(GOLD_DARK);
        g2.drawOval(cx - r, cy - r, 2 * r, 2 * r);

        g2.setColor(GOLD);
        g2.fill(star(cx, y + 22 * s, 7 * s));
    }

    static BufferedImage icon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = Widgets.smooth(img.getGraphics());
        paintCrest(g2, 0, 0, size);
        g2.dispose();
        return img;
    }

    private static Path2D polygon(double cx, double cy, double r, double startDeg, int n) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < n; i++) {
            double a = Math.toRadians(startDeg + 360.0 / n * i);
            if (i == 0) {
                p.moveTo(cx + Math.cos(a) * r, cy + Math.sin(a) * r);
            } else {
                p.lineTo(cx + Math.cos(a) * r, cy + Math.sin(a) * r);
            }
        }
        p.closePath();
        return p;
    }

    private static Path2D star(double cx, double cy, double r) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double a = Math.toRadians(-90 + 36 * i);
            double rr = i % 2 == 0 ? r : r * 0.45;
            if (i == 0) {
                p.moveTo(cx + Math.cos(a) * rr, cy + Math.sin(a) * rr);
            } else {
                p.lineTo(cx + Math.cos(a) * rr, cy + Math.sin(a) * rr);
            }
        }
        p.closePath();
        return p;
    }

    private static String spaced(String text) {
        return String.join(" ", text.split(""));
    }

    private static void drawCenteredAt(Graphics2D g2, String text, int w, int baseline) {
        g2.drawString(text, (w - g2.getFontMetrics().stringWidth(text)) / 2, baseline);
    }
}
