package superleague.gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.function.DoubleConsumer;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.Timer;

/**
 * Tiny animation helpers for the UI (hover fades, sliding tab underline, screen cross-fade).
 * Only visual: no game logic lives here. Each tween owns one short Swing Timer that stops itself.
 */
final class Anim {
    static final int FRAME_MS = 15;

    private Anim() {
    }

    static double easeOut(double t) {
        double u = 1 - Math.max(0, Math.min(1, t));
        return 1 - u * u * u;
    }

    static Color mix(Color a, Color b, double t) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        double k = Math.max(0, Math.min(1, t));
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * k),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * k),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * k),
                (int) Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * k));
    }

    /** A value that glides toward its target and repaints its owner while moving. */
    static final class Value {
        private final JComponent owner;
        private final int durationMs;
        private double from;
        private double to;
        private double now;
        private long start;
        private Timer timer;

        Value(JComponent owner, double initial, int durationMs) {
            this.owner = owner;
            this.durationMs = durationMs;
            this.from = initial;
            this.to = initial;
            this.now = initial;
        }

        double get() {
            return now;
        }

        double target() {
            return to;
        }

        void snap(double v) {
            stop();
            from = to = now = v;
        }

        void animateTo(double target) {
            if (target == to) {
                return;
            }
            if (!owner.isShowing()) {
                snap(target);
                return;
            }
            from = now;
            to = target;
            start = System.currentTimeMillis();
            if (timer == null) {
                timer = new Timer(FRAME_MS, e -> step());
                timer.setCoalesce(true);
            }
            if (!timer.isRunning()) {
                timer.start();
            }
        }

        private void step() {
            double t = (System.currentTimeMillis() - start) / (double) durationMs;
            now = from + (to - from) * easeOut(t);
            if (t >= 1) {
                now = to;
                stop();
            }
            owner.repaint();
        }

        private void stop() {
            if (timer != null) {
                timer.stop();
            }
        }
    }

    /** Runs a one-off 0→1 tween. */
    static Timer run(int durationMs, DoubleConsumer frame, Runnable done) {
        long start = System.currentTimeMillis();
        Timer[] holder = new Timer[1];
        holder[0] = new Timer(FRAME_MS, e -> {
            double t = Math.min(1, (System.currentTimeMillis() - start) / (double) durationMs);
            frame.accept(easeOut(t));
            if (t >= 1) {
                holder[0].stop();
                if (done != null) {
                    done.run();
                }
            }
        });
        holder[0].setCoalesce(true);
        holder[0].start();
        return holder[0];
    }

    /**
     * Cross-fade: snapshot what the root pane shows now, let the caller swap content,
     * then fade the old snapshot out over the new screen (glass pane overlay).
     */
    static void crossFade(JRootPane root, Runnable swap) {
        if (root == null || !root.isShowing() || root.getWidth() <= 0 || root.getHeight() <= 0) {
            swap.run();
            return;
        }
        BufferedImage shot = new BufferedImage(root.getWidth(), root.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = shot.createGraphics();
        root.getLayeredPane().paint(g);
        g.dispose();
        swap.run();
        float[] alpha = {1f};
        JComponent veil = new JComponent() {
            @Override
            protected void paintComponent(Graphics gr) {
                Graphics2D g2 = (Graphics2D) gr.create();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha[0]));
                g2.drawImage(shot, 0, 0, null);
                g2.dispose();
            }
        };
        java.awt.Component oldGlass = root.getGlassPane();
        root.setGlassPane(veil);
        veil.setVisible(true);
        run(240, t -> {
            alpha[0] = (float) (1 - t);
            veil.repaint();
        }, () -> {
            veil.setVisible(false);
            root.setGlassPane(oldGlass);
        });
    }
}
