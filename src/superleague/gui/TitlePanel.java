package superleague.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Component;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

class TitlePanel extends JPanel {

    TitlePanel(Runnable onNewGame) {
        super(new GridBagLayout());
        JPanel column = new JPanel() {
            private float alpha = 0f;
            private int rise = 18;
            private boolean started;

            @Override
            public void addNotify() {
                super.addNotify();
                if (!started) {
                    started = true;
                    // one-time entrance: fade in while rising 18px
                    Anim.run(650, t -> {
                        alpha = (float) t;
                        rise = (int) Math.round(18 * (1 - t));
                        repaint();
                    }, null);
                }
            }

            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));
                g2.translate(0, rise);
                super.paint(g2);
                g2.dispose();
            }
        };
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        Logo logo = new Logo();
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(logo);
        column.add(Box.createVerticalStrut(8));
        JLabel tagline = Theme.label("18개 구단 · 34라운드 · 5시즌. 당신의 축구 철학을 증명하세요.",
                Font.PLAIN, 15, Theme.ON_DARK_MUTED);
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(tagline);
        column.add(Box.createVerticalStrut(36));

        JButton start = Theme.primaryButton("새 게임 시작");
        start.setFont(Theme.font(Font.BOLD, 16));
        start.setAlignmentX(Component.CENTER_ALIGNMENT);
        start.addActionListener(e -> onNewGame.run());
        ((Widgets.FlatButton) start).fixSize(220, 46);
        column.add(start);
        column.add(Box.createVerticalStrut(10));
        JButton quit = Theme.secondaryButton("종료");
        quit.setAlignmentX(Component.CENTER_ALIGNMENT);
        ((Widgets.FlatButton) quit).fixSize(220, 46);
        quit.addActionListener(e -> System.exit(0));
        column.add(quit);
        java.awt.GridBagConstraints center = new java.awt.GridBagConstraints();
        center.gridx = 0;
        center.gridy = 0;
        add(column, center);
        java.awt.GridBagConstraints corner = new java.awt.GridBagConstraints();
        corner.gridx = 0;
        corner.gridy = 0;
        corner.anchor = java.awt.GridBagConstraints.FIRST_LINE_END;
        corner.insets = new java.awt.Insets(18, 0, 0, 18);
        corner.weightx = 1;
        corner.weighty = 1;
        add(Theme.themeToggle(this), corner);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setPaint(new GradientPaint(0, 0, Theme.NAVY_LIGHT, 0, getHeight(), Theme.NAVY));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(new Color(255, 255, 255, 14));
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        g2.drawOval(cx - 260, cy - 260, 520, 520);
        g2.drawLine(cx, 0, cx, getHeight());
        g2.dispose();
    }
}
