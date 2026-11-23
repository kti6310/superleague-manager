package superleague.gui;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import superleague.game.Season;
import superleague.model.Tactic;
import superleague.model.Team;
import superleague.sim.Fixture;

/**
 * Pre-match screen for the "라운드 결과" tab: who we play, when, and how the two sides compare,
 * laid out as cards/bars instead of sentences. Display only — reads Season/Team, changes nothing.
 */
final class MatchPreview extends JPanel {
    private float appear;

    MatchPreview(Season season, Team team, String lastResult, Runnable openTactics) {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        Fixture next = season.nextUserFixture();
        int played = season.roundsPlayed();
        if (season.isFinished()) {
            column.add(center(Theme.label("시즌 종료", Font.BOLD, 26, Theme.TEXT)));
            column.add(Box.createVerticalStrut(6));
            column.add(center(Theme.label(season.positionLabel(team), Font.PLAIN, 14, Theme.MUTED)));
        } else if (next == null) {
            LocalDate date = season.dateOfRound(played);
            column.add(headerRow(Season.roundLabel(played) + " · " + Season.formatDate(date),
                    ChronoUnit.DAYS.between(season.today(), date)));
            column.add(Box.createVerticalStrut(28));
            column.add(center(Theme.label("이번 라운드는 휴식", Font.BOLD, 26, Theme.TEXT)));
            column.add(Box.createVerticalStrut(6));
            column.add(center(Theme.label(season.isPostseason() ? season.positionLabel(team)
                    : "9팀 조라 라운드마다 한 팀씩 쉽니다 · 현재 " + season.positionLabel(team), Font.PLAIN, 14, Theme.MUTED)));
        } else {
            buildMatch(column, season, team, next, played);
        }

        if (lastResult != null) {
            column.add(Box.createVerticalStrut(14));
            column.add(center(Theme.label(lastResult, Font.PLAIN, 12, Theme.MUTED)));
        }
        column.add(Box.createVerticalStrut(18));
        column.add(hints(openTactics, !season.isFinished()));
        add(column, BorderLayout.NORTH);
    }

    private void buildMatch(JPanel column, Season season, Team team, Fixture next, int played) {
        Team home = next.home();
        Team away = next.away();
        Team opp = next.opponentOf(team);
        opp.autoPick();
        Kits.Match kits = Kits.forMatch(home, away);
        LocalDate date = season.dateOfRound(played);
        long dDay = ChronoUnit.DAYS.between(season.today(), date);

        column.add(headerRow(Season.roundLabel(played) + " · " + Season.formatDate(date)
                + " · " + (next.neutral() ? "중립 구장" : next.home() == team ? "우리 홈" : "원정 경기"), dDay));
        column.add(Box.createVerticalStrut(14));

        // matchup: home | VS | away
        JPanel matchup = new JPanel(new GridLayout(1, 3, 10, 0));
        matchup.setOpaque(false);
        matchup.add(teamBlock(season, home, kits.home(), home == team));
        JPanel vs = new JPanel();
        vs.setOpaque(false);
        vs.setLayout(new BoxLayout(vs, BoxLayout.Y_AXIS));
        vs.add(Box.createVerticalStrut(18));
        vs.add(center(Theme.label("VS", Font.BOLD, 30, Theme.BORDER_STRONG)));
        matchup.add(vs);
        matchup.add(teamBlock(season, away, kits.away(), away == team));
        column.add(matchup);
        column.add(Box.createVerticalStrut(16));

        // comparison bars (our side in blue)
        Widgets.RoundPanel compare = new Widgets.RoundPanel(new GridLayout(0, 1, 0, 8), 12);
        compare.setBackground(Theme.SUBTLE);
        compare.setLine(null, 1);
        compare.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        boolean userHome = home == team;
        compare.add(new CompareBar("예상 경기력", home.performance(), away.performance(), "%.1f", userHome));
        compare.add(new CompareBar("조별 승점", home.points(), away.points(), "%.0f", userHome));
        compare.add(new CompareBar("득점", home.goalsFor(), away.goalsFor(), "%.0f", userHome));
        column.add(compare);
        column.add(Box.createVerticalStrut(12));

        // tactics as chips instead of a sentence
        Tactic style = opp.preferredTactic();
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        chips.setOpaque(false);
        chips.add(new Widgets.Chip("상대 운영 · " + style.label(), Theme.MUTED));
        chips.add(new Widgets.Chip("상대 포메이션 · " + opp.formation(), Theme.MUTED));
        chips.add(new Widgets.Chip("우리 운영 · " + team.tactic().label(), Theme.ACCENT));
        column.add(chips);
        column.add(Box.createVerticalStrut(10));

        boolean ready = team.tactic().counters(style);
        String tip = ready ? "지금 우리 운영이 상대(" + style.family().label() + ")에 유리합니다"
                : "스카우트 조언: 상대가 " + style.family().label() + "이라 " + style.family().beatenBy().label() + " 운영이 유리합니다";
        Color tone = ready ? Theme.GOOD : Theme.WARN;
        Widgets.RoundPanel callout = new Widgets.RoundPanel(new BorderLayout(), 10);
        callout.setBackground(Anim.mix(Theme.CARD, tone, Theme.isDark() ? 0.18 : 0.10));
        callout.setLine(Anim.mix(Theme.CARD, tone, 0.45), 1);
        callout.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        JLabel tipLabel = Theme.label((ready ? "✓  " : "!  ") + tip, Font.BOLD, 13, tone);
        tipLabel.setHorizontalAlignment(JLabel.CENTER);
        callout.add(tipLabel);
        column.add(callout);
    }

    private static JComponent headerRow(String text, long dDay) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.add(Theme.label("다음 경기  ·  " + text, Font.BOLD, 13, Theme.MUTED), BorderLayout.WEST);
        Widgets.Chip pill = new Widgets.Chip(dDay <= 0 ? "오늘 경기!" : "D-" + dDay, dDay <= 0 ? Theme.BAD : Theme.ACCENT);
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        wrap.setOpaque(false);
        wrap.add(pill);
        row.add(wrap, BorderLayout.EAST);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        return row;
    }

    private static JComponent teamBlock(Season season, Team t, Kits.Kit kit, boolean mine) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JComponent dot = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Widgets.smooth(g);
                PitchView.drawKitDot(g2, getWidth() / 2, getHeight() / 2 - 2, 26, kit);
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(64, 64));
        dot.setMaximumSize(new Dimension(64, 64));
        p.add(center(dot));
        p.add(Box.createVerticalStrut(6));
        p.add(center(Theme.label(t.name(), Font.BOLD, 19, Theme.TEXT)));
        p.add(Box.createVerticalStrut(4));
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        chips.setOpaque(false);
        chips.add(new Widgets.Chip(t.tier().name() + "등급", Theme.tierColor(t.tier())));
        if (mine) {
            chips.add(new Widgets.Chip("내 팀", Theme.ACCENT));
        }
        p.add(center(chips));
        p.add(Box.createVerticalStrut(4));
        p.add(center(Theme.label(season.positionLabel(t) + " · " + t.won() + "승 " + t.drawn() + "무 " + t.lost() + "패",
                Font.PLAIN, 12, Theme.MUTED)));
        return p;
    }

    private static JComponent hints(Runnable openTactics, boolean running) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        row.setOpaque(false);
        if (running) {
            row.add(new KeyCap("Space"));
            row.add(Theme.label("다음 날 / 경기 진행", Font.PLAIN, 12, Theme.MUTED));
            row.add(Box.createHorizontalStrut(10));
            row.add(new KeyCap("경기일까지 ▶▶"));
            row.add(Theme.label("한 번에 이동", Font.PLAIN, 12, Theme.MUTED));
        }
        if (openTactics != null && running) {
            row.add(Box.createHorizontalStrut(10));
            JButton tactics = Theme.secondaryButton("전술 조정하기");
            tactics.addActionListener(e -> openTactics.run());
            row.add(tactics);
        }
        return row;
    }

    private static JComponent center(JComponent c) {
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
        if (c instanceof JLabel l) {
            l.setHorizontalAlignment(JLabel.CENTER);
        }
        return c;
    }

    // ---- entrance animation ---------------------------------------------------------------------

    @Override
    public void addNotify() {
        super.addNotify();
        appear = 0f;
        Anim.run(380, t -> {
            appear = (float) t;
            repaint();
        }, null);
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, appear))));
        g2.translate(0, Math.round((1 - appear) * 14));
        super.paint(g2);
        g2.dispose();
    }

    /** "label  [home value ███████|░░░░ away value]" with the split growing in on first show. */
    private static final class CompareBar extends JComponent {
        private final String label;
        private final double home;
        private final double away;
        private final String format;
        private final boolean userHome;
        private float grow;

        CompareBar(String label, double home, double away, String format, boolean userHome) {
            this.label = label;
            this.home = home;
            this.away = away;
            this.format = format;
            this.userHome = userHome;
            setPreferredSize(new Dimension(300, 34));
        }

        @Override
        public void addNotify() {
            super.addNotify();
            grow = 0f;
            Anim.run(700, t -> {
                grow = (float) t;
                repaint();
            }, null);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Widgets.smooth(g);
            int w = getWidth();
            g2.setFont(Theme.font(Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            String hs = String.format(format, home);
            String as = String.format(format, away);
            g2.setColor(Theme.MUTED);
            g2.drawString(label, (w - fm.stringWidth(label)) / 2, fm.getAscent());
            g2.setColor(home >= away ? Theme.TEXT : Theme.MUTED);
            g2.drawString(hs, 0, fm.getAscent());
            g2.setColor(away >= home ? Theme.TEXT : Theme.MUTED);
            g2.drawString(as, w - fm.stringWidth(as), fm.getAscent());

            int y = fm.getHeight() + 4;
            int h = 8;
            double total = Math.max(0, home) + Math.max(0, away);
            double share = total <= 0 ? 0.5 : Math.max(0, home) / total;
            double shown = 0.5 + (share - 0.5) * grow;
            int split = (int) Math.round(w * shown);
            Color mine = Theme.ACCENT;
            Color theirs = Theme.BORDER_STRONG;
            g2.setColor(Theme.TRACK);
            g2.fillRoundRect(0, y, w, h, h, h);
            if (split > 0) {
                g2.setColor(userHome ? mine : theirs);
                g2.fillRoundRect(0, y, Math.max(h, split), h, h, h);
            }
            if (split < w) {
                g2.setColor(userHome ? theirs : mine);
                g2.fillRoundRect(Math.min(w - h, split), y, Math.max(h, w - split), h, h, h);
            }
            if (split > 0 && split < w) {
                g2.setColor(Theme.SUBTLE);
                g2.fillRect(split - 1, y - 1, 2, h + 2);
            }
            g2.dispose();
        }
    }

    /** Small keyboard-key looking label. */
    private static final class KeyCap extends JComponent {
        private final String text;

        KeyCap(String text) {
            this.text = text;
            setFont(Theme.font(Font.BOLD, 11));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(text) + 16, 24);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Widgets.smooth(g);
            g2.setColor(Theme.BORDER_STRONG);
            g2.fillRoundRect(0, 2, getWidth() - 1, getHeight() - 3, 7, 7);
            g2.setColor(Theme.CARD);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 4, 7, 7);
            g2.setColor(Theme.BORDER_STRONG);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 4, 7, 7);
            g2.setColor(Theme.TEXT);
            g2.setFont(getFont());
            Widgets.drawCentered(g2, text, getWidth(), getHeight() - 4);
            g2.dispose();
        }
    }
}
