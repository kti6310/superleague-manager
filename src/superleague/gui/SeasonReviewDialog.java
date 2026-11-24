package superleague.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import superleague.game.Ending;
import superleague.game.Reactions;
import superleague.game.Season;
import superleague.game.Verdict;
import superleague.model.Player;
import superleague.model.Position;
import superleague.model.Team;

final class SeasonReviewDialog extends JDialog {

    private SeasonReviewDialog(Component parent, Season season, Ending ending) {
        super(SwingUtilities.getWindowAncestor(parent), season.seasonLabel() + " 시즌 리뷰", ModalityType.APPLICATION_MODAL);
        Team user = season.userTeam();
        List<Team> table = season.standings();
        Team champion = table.get(0);
        Team runnerUp = table.get(1);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(Theme.NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(18, 26, 16, 26));
        header.add(Theme.label(season.seasonLabel() + " 슈퍼리그 시즌 리뷰", Font.PLAIN, 14, Theme.ON_DARK_MUTED));
        header.add(Theme.label("우승  " + champion.name(), Font.BOLD, 30, Logo.GOLD));
        header.add(Theme.label("준우승 " + runnerUp.name() + "  ·  우리 팀 " + user.name() + " " + season.positionLabel(user),
                Font.PLAIN, 14, Color.WHITE));

        DefaultTableModel model = Tables.model("순위", "구단", "등급", "결과", "조별 승점", "전적", "득실");
        for (int i = 0; i < table.size(); i++) {
            Team t = table.get(i);
            model.addRow(new Object[]{i + 1, t.name(), t.tier().name(), season.positionLabel(t), t.points(),
                    t.won() + "-" + t.drawn() + "-" + t.lost(), String.format("%+d", t.goalDifference())});
        }
        JTable standings = Tables.table(model, row -> table.get(row) == user, 1, 3);
        Tables.widths(standings, 40, 150, 40, 120, 70, 96, 50);
        JPanel left = Theme.card("최종 순위", Tables.scroll(standings));

        List<Player> all = season.allPlayers();
        String league = "<html>"
                + award("득점왕", season.best(all, Player::goals), season, p -> p.goals() + "골")
                + award("도움왕", season.best(all, Player::assists), season, p -> p.assists() + "도움")
                + award("클린시트", season.best(all.stream().filter(p -> p.position() == Position.GK).toList(),
                Player::cleanSheets), season, p -> p.cleanSheets() + "경기 무실점")
                + award("시즌 MVP", season.best(all, season::mvpScore), season, SeasonReviewDialog::summary)
                + award("영플레이어", season.best(all.stream().filter(p -> p.age() <= 21).toList(), season::mvpScore),
                season, p -> p.age() + "세 · " + summary(p))
                + "</html>";
        List<Player> mine = user.squad();
        String ours = "<html>"
                + award("팀 득점왕", season.best(mine, Player::goals), null, p -> p.goals() + "골")
                + award("팀 도움왕", season.best(mine, Player::assists), null, p -> p.assists() + "도움")
                + award("팀 클린시트", season.best(mine.stream().filter(p -> p.position() == Position.GK).toList(),
                Player::cleanSheets), null, p -> p.cleanSheets() + "경기 무실점")
                + award("팀 MVP", season.best(mine, season::mvpScore), null, SeasonReviewDialog::summary)
                + "</html>";
        int rank = season.rankOf(user);
        Verdict verdict = Verdict.of(user.tier(), rank);
        String owner = String.format("<html><b>%s</b> · 목표 %s, 결과 %s<br><b>%s</b> — %s</html>", verdict.label(),
                user.tier().targetLabel(), season.positionLabel(user), ending.title(), ending.description());
        StringBuilder press = new StringBuilder("<html>");
        Reactions.season(user, season.positionLabel(user), rank, ThreadLocalRandom.current())
                .forEach(line -> press.append(line).append("<br>"));
        press.append("</html>");

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);
        String[][] cards = {{"리그 타이틀", league}, {"우리 팀 타이틀", ours}, {"구단주 평가", owner},
                {"팬 · 언론 평가", press.toString()}};
        for (int i = 0; i < cards.length; i++) {
            if (i > 0) {
                right.add(javax.swing.Box.createVerticalStrut(10));
            }
            JPanel card = Theme.card(cards[i][0], text(cards[i][1]));
            card.setAlignmentX(LEFT_ALIGNMENT);
            card.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
            right.add(card);
        }

        JPanel body = new JPanel(new GridLayout(1, 2, 14, 0));
        body.setBackground(Theme.BG);
        body.setBorder(BorderFactory.createEmptyBorder(14, 16, 8, 16));
        body.add(left);
        body.add(right);

        JButton next = Theme.primaryButton("다음");
        next.addActionListener(e -> dispose());
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 10));
        foot.setBackground(Theme.BG);
        foot.add(next);

        JPanel root = new JPanel(new BorderLayout());
        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        root.add(foot, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(next);
        setSize(1180, 780);
        setLocationRelativeTo(parent);
    }

    static void show(Component parent, Season season, Ending ending) {
        new SeasonReviewDialog(parent, season, ending).setVisible(true);
    }

    private static String summary(Player p) {
        return p.goals() + "골 " + p.assists() + "도움" + (p.cleanSheets() > 0 ? " " + p.cleanSheets() + "무실점" : "")
                + " · " + p.appearances() + "경기";
    }

    private static String award(String title, Player p, Season season, java.util.function.Function<Player, String> stat) {
        if (p == null) {
            return "<b>" + title + "</b> &nbsp;-<br>";
        }
        String team = season == null ? "" : " <font color='#64748B'>(" + season.teamOf(p).name() + ")</font>";
        return String.format("<b>%s</b> &nbsp;%s%s &nbsp;<font color='#2563EB'>%s</font><br>", title, p.name(), team,
                stat.apply(p));
    }

    private static JComponent text(String html) {
        JLabel l = Theme.label(html, Font.PLAIN, 13, Theme.TEXT);
        l.setVerticalAlignment(JLabel.TOP);
        return l;
    }
}
