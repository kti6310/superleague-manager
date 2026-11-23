package superleague.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import superleague.sim.MatchEvent;

class GoalTimeline extends JComponent {
    private static final int PAD = 28;
    private static final int MAX_MINUTE = 95;
    private static final Color OWN_GOAL = new Color(0xDC2626);

    private final List<MatchEvent> goals = new ArrayList<>();

    GoalTimeline() {
        setPreferredSize(new Dimension(600, 150));
    }

    void setEvents(List<MatchEvent> events) {
        goals.clear();
        for (MatchEvent e : events) {
            if (e.type() == MatchEvent.Type.GOAL) {
                goals.add(e);
            }
        }
        repaint();
    }

    private int x(double minute) {
        return (int) Math.round(PAD + (getWidth() - PAD * 2) * Math.min(MAX_MINUTE, minute) / MAX_MINUTE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Widgets.smooth(g);
        int w = getWidth();
        int h = getHeight();
        int midY = h / 2;

        g2.setColor(Theme.CARD);
        g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(0xCBD5E1));
        g2.drawLine(x(0), midY, x(MAX_MINUTE), midY);
        g2.setFont(Theme.font(Font.BOLD, 10));
        FontMetrics fm = g2.getFontMetrics();
        for (int m = 0; m <= 90; m += 15) {
            int xx = x(m);
            g2.setColor(new Color(0x94A3B8));
            g2.drawLine(xx, midY - 4, xx, midY + 4);
            String label = m == 45 ? "HT" : m + "'";
            g2.drawString(label, xx - fm.stringWidth(label) / 2, midY + 16);
        }

        int[] lastRight = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        g2.setFont(Theme.font(Font.BOLD, 11));
        FontMetrics f2 = g2.getFontMetrics();
        for (MatchEvent e : goals) {
            boolean home = e.side() == MatchEvent.Side.HOME;
            int bx = x(e.minute() + e.extra() * 0.8);
            int by = home ? midY - 14 : midY + 14;
            drawBall(g2, bx, by, 8, e.ownGoal());

            String name = e.player() == null ? "" : e.player().name() + (e.ownGoal() ? " (자책)" : "");
            String label = e.clock() + " " + name;
            int tw = f2.stringWidth(label);
            int lx = Math.max(4, Math.min(w - tw - 4, bx - tw / 2));
            int lane = home ? 0 : 2;
            if (lx < lastRight[lane] + 6) {
                lane++;
            }
            lastRight[lane] = lx + tw;
            int far = lane % 2;
            int ly = home ? by - 14 - far * 15 : by + 22 + far * 15;
            g2.setColor(e.ownGoal() ? OWN_GOAL : Theme.TEXT);
            g2.drawString(label, lx, ly);
        }
        g2.dispose();
    }

    static void drawBall(Graphics2D g2, int cx, int cy, int r, boolean ownGoal) {
        g2.setColor(new Color(0, 0, 0, 50));
        g2.fillOval(cx - r + 1, cy - r + 2, r * 2, r * 2);
        g2.setColor(ownGoal ? OWN_GOAL : Color.WHITE);
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
        Color patch = ownGoal ? new Color(0x7F1D1D) : new Color(0x1F2937);
        g2.setColor(patch);
        g2.fillPolygon(pentagon(cx, cy, r * 0.42, -Math.PI / 2));
        for (int i = 0; i < 5; i++) {
            double a = -Math.PI / 2 + i * 2 * Math.PI / 5;
            int px = (int) Math.round(cx + Math.cos(a) * r * 0.92);
            int py = (int) Math.round(cy + Math.sin(a) * r * 0.92);
            g2.fillPolygon(pentagon(px, py, r * 0.28, a + Math.PI / 2));
        }
        g2.setStroke(new BasicStroke(1.2f));
        g2.setColor(patch);
        g2.drawOval(cx - r, cy - r, r * 2, r * 2);
    }

    private static Polygon pentagon(double cx, double cy, double r, double start) {
        Polygon p = new Polygon();
        for (int i = 0; i < 5; i++) {
            double a = start + i * 2 * Math.PI / 5;
            p.addPoint((int) Math.round(cx + Math.cos(a) * r), (int) Math.round(cy + Math.sin(a) * r));
        }
        return p;
    }
}
