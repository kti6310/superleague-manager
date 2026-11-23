package superleague.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.util.List;
import java.util.Random;
import javax.swing.JComponent;
import javax.swing.Timer;
import superleague.model.Formation;
import superleague.model.Player;
import superleague.model.Team;
import superleague.sim.MatchEvent;

class PitchView extends JComponent {
    private static final int N = 11;

    private final Team home;
    private final Team away;
    private final int homePossession;
    private final Random random = new Random();
    private final double[][] cur = new double[N * 2][2];
    private final double[][] target = new double[N * 2][2];
    private final double[][] vel = new double[N * 2][2];
    private final double[] ballVel = new double[2];
    private final double[] ball = {0.5, 0.5};
    private final double[] ballTarget = {0.5, 0.5};
    private final Timer anim;

    private boolean secondHalf;
    private MatchEvent.Side attacking = MatchEvent.Side.NONE;
    private double push = 0.5;
    private int highlight = -1;
    private String banner;
    private int bannerTicks;

    PitchView(Team home, Team away, int homePossession) {
        this.home = home;
        this.away = away;
        this.homePossession = homePossession;
        setPreferredSize(new Dimension(580, 375));
        setOpaque(false);
        kickoff();
        for (int i = 0; i < N * 2; i++) {
            cur[i][0] = target[i][0];
            cur[i][1] = target[i][1];
        }
        anim = new Timer(16, e -> animate());
    }

    void start() {
        anim.start();
    }

    void stop() {
        anim.stop();
    }

    private boolean homeAttacksRight() {
        return !secondHalf;
    }

    private Team team(int idx) {
        return idx < N ? home : away;
    }

    private Formation.Slot slot(int idx) {
        return team(idx).slots().get(idx % N);
    }

    private boolean attacksRight(int idx) {
        return idx < N == homeAttacksRight();
    }

    private void place(int idx, double along, double across) {
        boolean right = attacksRight(idx);
        target[idx][0] = clamp(right ? along : 1 - along);
        target[idx][1] = clamp(right ? across : 1 - across);
    }

    private static double clamp(double v) {
        return Math.max(0.03, Math.min(0.97, v));
    }

    void kickoff() {
        for (int i = 0; i < N * 2; i++) {
            Formation.Slot s = slot(i);
            place(i, (1 - s.y()) * 0.48, 0.08 + s.x() * 0.84);
        }
        ballTarget[0] = 0.5;
        ballTarget[1] = 0.5;
        push = 0.5;
        attacking = MatchEvent.Side.NONE;
        highlight = -1;
    }

    void switchSides() {
        secondHalf = !secondHalf;
        banner = "진영 교체";
        bannerTicks = 45;
        kickoff();
    }

    void step(List<MatchEvent> fresh, List<MatchEvent> upcoming) {
        MatchEvent key = null;
        for (MatchEvent e : fresh) {
            if (e.side() != MatchEvent.Side.NONE && e.type() != MatchEvent.Type.INFO && e.type() != MatchEvent.Type.SUB) {
                key = e;
            }
        }
        if (key != null) {
            showEvent(key);
            return;
        }
        MatchEvent.Side side = attacking;
        MatchEvent.Side soon = upcoming.stream().filter(e -> e.side() != MatchEvent.Side.NONE)
                .map(MatchEvent::side).findFirst().orElse(MatchEvent.Side.NONE);
        if (soon != MatchEvent.Side.NONE && random.nextDouble() < 0.6) {
            side = soon;
        } else if (side == MatchEvent.Side.NONE || random.nextDouble() < 0.3) {
            side = random.nextInt(100) < homePossession ? MatchEvent.Side.HOME : MatchEvent.Side.AWAY;
        }
        if (side != attacking) {
            push = 1 - push;
            attacking = side;
        }
        push = Math.max(0.15, Math.min(0.85, push + 0.05 + random.nextDouble() * 0.12));
        highlight = -1;
        layout(0.12 + random.nextDouble() * 0.76, -1);
    }

    private void showEvent(MatchEvent e) {
        attacking = e.side();
        int carrier = carrierOf(e);
        switch (e.type()) {
            case GOAL -> {
                push = 0.95;
                layout(0.5, e.ownGoal() ? -1 : carrier);
                setBall(attackIndexBase(), 1.0, 0.5);
                if (e.ownGoal() && carrier >= 0) {
                    place(carrier, 0.07, 0.45);
                }
                banner = e.ownGoal() ? "자책골" : "GOAL!";
                bannerTicks = 50;
            }
            case SAVE, BIG_SAVE -> {
                push = 0.88;
                layout(0.5, carrier);
                int gk = attacking == MatchEvent.Side.HOME ? N : 0;
                ballTarget[0] = target[gk][0];
                ballTarget[1] = target[gk][1];
            }
            case MISS, WOODWORK -> {
                push = 0.88;
                layout(0.5, carrier);
                setBall(attackIndexBase(), 1.0, random.nextBoolean() ? 0.36 : 0.64);
            }
            case BLOCK -> {
                push = 0.8;
                layout(0.5, carrier);
            }
            default -> {
                layout(0.2 + random.nextDouble() * 0.6, carrier);
            }
        }
        highlight = carrier;
        if (e.type() == MatchEvent.Type.RED) {
            banner = "퇴장";
            bannerTicks = 40;
        }
    }

    private int attackIndexBase() {
        return attacking == MatchEvent.Side.HOME ? 0 : N;
    }

    private void setBall(int idx, double along, double across) {
        boolean right = attacksRight(idx);
        ballTarget[0] = right ? along : 1 - along;
        ballTarget[1] = right ? across : 1 - across;
    }

    private int carrierOf(MatchEvent e) {
        for (int i = 0; i < N * 2; i++) {
            if (e.player() != null && team(i).slotPlayer(i % N) == e.player()) {
                return i;
            }
        }
        return -1;
    }

    private void layout(double ballAcross, int carrier) {
        for (int i = 0; i < N * 2; i++) {
            Formation.Slot s = slot(i);
            boolean mine = (i < N) == (attacking == MatchEvent.Side.HOME);
            double ownBall = mine ? push : 1 - push;
            double depth = 1 - s.y();
            double along = i % N == 0
                    ? 0.04 + Math.max(0, ownBall - 0.5) * 0.14
                    : 0.06 + depth * 0.5 + (ownBall - 0.5) * 0.5 + (mine ? 0.06 : -0.02);
            double across = 0.08 + s.x() * 0.84;
            across += (ballAcrossFor(i, ballAcross) - across) * (mine ? 0.15 : 0.3);
            place(i, along + jitter(), across + jitter());
        }
        if (carrier >= 0) {
            setBall(carrier, push, ballAcross);
            place(carrier, push - 0.02, ballAcross);
        } else {
            int base = attackIndexBase();
            setBall(base, push, ballAcross);
            int nearest = base + 1;
            double d = Double.MAX_VALUE;
            for (int i = base + 1; i < base + N; i++) {
                double dd = Math.hypot(target[i][0] - ballTarget[0], target[i][1] - ballTarget[1]);
                if (dd < d) {
                    d = dd;
                    nearest = i;
                }
            }
            target[nearest][0] = clamp(ballTarget[0] + (attacksRight(nearest) ? -0.015 : 0.015));
            target[nearest][1] = ballTarget[1];
        }
    }

    private double ballAcrossFor(int idx, double across) {
        boolean sameFrame = attacksRight(idx) == attacksRight(attackIndexBase());
        return sameFrame ? across : 1 - across;
    }

    private double jitter() {
        return (random.nextDouble() - 0.5) * 0.03;
    }

    private void animate() {
        boolean moving = false;
        for (int i = 0; i < N * 2; i++) {
            for (int k = 0; k < 2; k++) {
                moving |= spring(cur[i], vel[i], target[i], k, 0.010, 0.16, 0.012);
            }
        }
        for (int k = 0; k < 2; k++) {
            moving |= spring(ball, ballVel, ballTarget, k, 0.030, 0.26, 0.03);
        }
        if (bannerTicks > 0) {
            bannerTicks--;
            moving = true;
        }
        if (moving) {
            repaint();
        }
    }

    // Damped spring: players accelerate and ease out instead of jumping toward each new target.
    private static boolean spring(double[] pos, double[] v, double[] goal, int k, double stiffness, double damping,
                                  double maxSpeed) {
        double d = goal[k] - pos[k];
        if (Math.abs(d) < 0.0004 && Math.abs(v[k]) < 0.0002) {
            v[k] = 0;
            return false;
        }
        v[k] = Math.max(-maxSpeed, Math.min(maxSpeed, v[k] * (1 - damping) + d * stiffness));
        pos[k] += v[k];
        return true;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Widgets.smooth(g);
        int w0 = getWidth();
        int h0 = getHeight();
        int w = Math.min(w0, (int) Math.round(h0 * 105 / 68.0));
        int h = (int) Math.round(w * 68 / 105.0);
        g2.translate((w0 - w) / 2, (h0 - h) / 2);
        int px = 16;
        int py = 12;
        int pw = w - px * 2;
        int ph = h - py * 2;

        g2.setColor(new Color(0x2E7D4F));
        g2.fillRoundRect(0, 0, w, h, 14, 14);
        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) {
                g2.setColor(new Color(0x348A58));
                g2.fillRect(px + pw * i / 10, py, pw / 10 + 1, ph);
            }
        }
        g2.setColor(new Color(255, 255, 255, 170));
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawRect(px, py, pw, ph);
        g2.drawLine(px + pw / 2, py, px + pw / 2, py + ph);
        int cr = ph / 6;
        g2.drawOval(px + pw / 2 - cr, py + ph / 2 - cr, cr * 2, cr * 2);
        int boxW = pw * 14 / 100;
        int boxH = ph * 56 / 100;
        int sixW = pw * 5 / 100;
        int sixH = ph * 26 / 100;
        g2.drawRect(px, py + (ph - boxH) / 2, boxW, boxH);
        g2.drawRect(px + pw - boxW, py + (ph - boxH) / 2, boxW, boxH);
        g2.drawRect(px, py + (ph - sixH) / 2, sixW, sixH);
        g2.drawRect(px + pw - sixW, py + (ph - sixH) / 2, sixW, sixH);
        int goalH = ph * 12 / 100;
        g2.setStroke(new BasicStroke(3f));
        g2.drawLine(px - 4, py + (ph - goalH) / 2, px - 4, py + (ph + goalH) / 2);
        g2.drawLine(px + pw + 4, py + (ph - goalH) / 2, px + pw + 4, py + (ph + goalH) / 2);

        g2.setFont(Theme.font(Font.BOLD, 11));
        g2.setColor(new Color(255, 255, 255, 200));
        String left = (homeAttacksRight() ? home.name() + " 공격 →" : "← " + home.name() + " 공격");
        String right = (homeAttacksRight() ? "← " + away.name() + " 공격" : away.name() + " 공격 →");
        g2.drawString((secondHalf ? "후반  " : "전반  ") + left, px + 8, py + 16);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(right, px + pw - 8 - fm.stringWidth(right), py + ph - 8);

        int r = Math.max(8, Math.min(12, ph / 24));
        for (int i = 0; i < N * 2; i++) {
            int sx = px + (int) Math.round(cur[i][0] * pw);
            int sy = py + (int) Math.round(cur[i][1] * ph);
            drawStone(g2, sx, sy, r, i < N, false);
            if (i == highlight) {
                g2.setColor(new Color(0xFBBF24));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawOval(sx - r - 3, sy - r - 3, r * 2 + 6, r * 2 + 6);
                Player p = team(i).slotPlayer(i % N);
                if (p != null) {
                    g2.setFont(Theme.font(Font.BOLD, 12));
                    FontMetrics f2 = g2.getFontMetrics();
                    int tw = f2.stringWidth(p.name());
                    int lx = Math.max(px, Math.min(px + pw - tw - 8, sx - tw / 2 - 4));
                    g2.setColor(new Color(15, 27, 51, 210));
                    g2.fillRoundRect(lx, sy - r - 26, tw + 8, 18, 8, 8);
                    g2.setColor(Color.WHITE);
                    g2.drawString(p.name(), lx + 4, sy - r - 13);
                }
            }
        }

        int bx = px + (int) Math.round(ball[0] * pw);
        int by = py + (int) Math.round(ball[1] * ph);
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(bx - 4, by - 2, 9, 9);
        g2.setColor(new Color(0xFBBF24));
        g2.fillOval(bx - 5, by - 5, 10, 10);
        g2.setColor(new Color(0x92400E));
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(bx - 5, by - 5, 10, 10);

        if (bannerTicks > 0 && banner != null) {
            float alpha = Math.min(1f, bannerTicks / 15f);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setFont(Theme.font(Font.BOLD, 40));
            FontMetrics f3 = g2.getFontMetrics();
            int tw = f3.stringWidth(banner);
            g2.setColor(new Color(15, 27, 51, 190));
            g2.fillRoundRect(w / 2 - tw / 2 - 20, h / 2 - 34, tw + 40, 64, 18, 18);
            g2.setColor(banner.equals("GOAL!") || banner.equals("진영 교체") ? new Color(0xFBBF24) : new Color(0xF87171));
            g2.drawString(banner, w / 2 - tw / 2, h / 2 + 12);
        }
        g2.dispose();
    }

    static void drawStone(Graphics2D g2, int cx, int cy, int r, boolean black, boolean red) {
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(cx - r + 1, cy - r + 2, r * 2, r * 2);
        float[] dist = {0f, 1f};
        Color[] colors = black
                ? new Color[]{new Color(0x6B6B6B), new Color(0x0D0D0D)}
                : new Color[]{Color.WHITE, new Color(0xCFCFCF)};
        g2.setPaint(new RadialGradientPaint(cx - r / 3f, cy - r / 3f, r * 1.4f, dist, colors));
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
        g2.setColor(black ? new Color(0x000000) : new Color(0x9A9A9A));
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(cx - r, cy - r, r * 2, r * 2);
        if (red) {
            g2.setColor(Theme.BAD);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(cx - r - 2, cy - r - 2, r * 2 + 4, r * 2 + 4);
        }
    }
}
