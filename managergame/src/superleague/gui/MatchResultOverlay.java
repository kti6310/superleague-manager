package superleague.gui;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import superleague.model.Team;
import superleague.sim.MatchResult;

/**
 * Full-time card shown over the match window: win/draw/loss, final score and scorers.
 * Display only — it reads the finished MatchResult and changes nothing.
 */
final class MatchResultOverlay extends JPanel {
    private float progress;

    private MatchResultOverlay(MatchResult result, Team userTeam, Kits.Match kits, Runnable onClose) {
        super(new GridBagLayout());
        setOpaque(false);
        // swallow clicks so the match window underneath can't be used while the card is up
        addMouseListener(new java.awt.event.MouseAdapter() {
        });

        Team home = result.fixture().home();
        Team away = result.fixture().away();
        boolean userHome = home == userTeam;
        int mine = userHome ? result.homeGoals() : result.awayGoals();
        int theirs = userHome ? result.awayGoals() : result.homeGoals();
        String headline;
        Color tone;
        if (mine > theirs) {
            headline = "승리!";
            tone = Theme.GOOD;
        } else if (mine < theirs) {
            headline = "패배";
            tone = Theme.BAD;
        } else {
            headline = "무승부";
            tone = Theme.WARN;
        }

        Widgets.RoundPanel card = new Widgets.RoundPanel(new BorderLayout(0, 14), 20) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = Widgets.smooth(g);
                g2.setColor(tone);
                g2.fillRoundRect(0, 0, getWidth() - 1, 6, 6, 6); // coloured top edge = result at a glance
                g2.dispose();
            }
        };
        card.setBorder(BorderFactory.createEmptyBorder(22, 32, 22, 32));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JLabel small = Theme.label("경기 종료 · FULL TIME", Font.BOLD, 12, Theme.MUTED);
        JLabel big = Theme.label(headline, Font.BOLD, 34, tone);
        small.setAlignmentX(Component.CENTER_ALIGNMENT);
        big.setAlignmentX(Component.CENTER_ALIGNMENT);
        top.add(small);
        top.add(Box.createVerticalStrut(4));
        top.add(big);
        card.add(top, BorderLayout.NORTH);

        JPanel middle = new JPanel(new BorderLayout(0, 12));
        middle.setOpaque(false);
        JPanel scoreRow = new JPanel(new GridLayout(1, 3, 16, 0));
        scoreRow.setOpaque(false);
        scoreRow.add(teamSide(home, kits.home(), home == userTeam, true));
        JLabel score = Theme.label(result.homeGoals() + "  :  " + result.awayGoals(), Font.BOLD, 40, Theme.TEXT);
        score.setHorizontalAlignment(JLabel.CENTER);
        scoreRow.add(score);
        scoreRow.add(teamSide(away, kits.away(), away == userTeam, false));
        middle.add(scoreRow, BorderLayout.NORTH);

        JPanel scorers = new JPanel(new GridLayout(1, 3, 16, 0));
        scorers.setOpaque(false);
        scorers.add(scorerList(result.homeScorers(), JLabel.RIGHT));
        scorers.add(Box.createHorizontalStrut(10));
        scorers.add(scorerList(result.awayScorers(), JLabel.LEFT));
        middle.add(scorers, BorderLayout.CENTER);
        card.add(middle, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttons.setOpaque(false);
        JButton review = Theme.secondaryButton("경기 기록 보기");
        JButton ok = Theme.primaryButton("확인");
        review.addActionListener(e -> dismiss());
        ok.addActionListener(e -> onClose.run());
        buttons.add(review);
        buttons.add(ok);
        card.add(buttons, BorderLayout.SOUTH);
        card.setPreferredSize(new Dimension(560, Math.max(300, card.getPreferredSize().height)));
        add(card);
    }

    private static JComponent teamSide(Team team, Kits.Kit kit, boolean mine, boolean home) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JComponent dot = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Widgets.smooth(g);
                PitchView.drawKitDot(g2, getWidth() / 2, getHeight() / 2 - 1, 14, kit);
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(36, 36));
        dot.setMaximumSize(new Dimension(36, 36));
        JLabel name = Theme.label(team.name(), Font.BOLD, 15, Theme.TEXT);
        JLabel tag = Theme.label((home ? "홈" : "원정") + (mine ? " · 내 팀" : ""), Font.PLAIN, 11,
                mine ? Theme.ACCENT : Theme.MUTED);
        for (JComponent c : new JComponent[]{dot, name, tag}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(c);
        }
        return p;
    }

    private static JComponent scorerList(List<String> scorers, int align) {
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 2));
        p.setOpaque(false);
        for (String s : scorers) {
            JLabel l = Theme.label("⚽ " + s, Font.PLAIN, 12, Theme.MUTED);
            if (l.getFont().canDisplayUpTo(l.getText()) != -1) {
                l.setText("· " + s); // fallback when the font has no ball glyph
            }
            l.setHorizontalAlignment(align);
            p.add(l);
        }
        return p;
    }

    private void dismiss() {
        Anim.run(180, t -> {
            progress = (float) (1 - t);
            repaint();
        }, () -> setVisible(false));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(10, 16, 30, Math.round(150 * progress)));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    @Override
    protected void paintChildren(Graphics g) {
        // card fades in while rising 24px and growing from 94% to 100%
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, progress))));
        double s = 0.94 + 0.06 * progress;
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        g2.translate(cx, cy + (1 - progress) * 24);
        g2.scale(s, s);
        g2.translate(-cx, -cy);
        super.paintChildren(g2);
        g2.dispose();
    }

    /** Shows the card on the window's glass pane. */
    static void show(JRootPane root, MatchResult result, Team userTeam, Kits.Match kits, Runnable onClose) {
        MatchResultOverlay overlay = new MatchResultOverlay(result, userTeam, kits, onClose);
        root.setGlassPane(overlay);
        overlay.setVisible(true);
        overlay.revalidate();
        Anim.run(420, t -> {
            overlay.progress = (float) t;
            overlay.repaint();
        }, null);
    }
}
