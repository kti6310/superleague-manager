package superleague.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import superleague.game.Ending;
import superleague.model.Player;
import superleague.model.Team;

final class EndingDialog {
    private EndingDialog() {
    }

    static boolean show(Component parent, Team team, String placement, Ending ending, Player topScorer,
                        java.util.List<String> career) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "시즌 엔딩", JDialog.ModalityType.APPLICATION_MODAL);
        boolean[] again = {false};

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(Theme.NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(22, 28, 20, 28));
        header.add(Theme.label("ENDING", Font.BOLD, 12, Theme.ON_DARK_MUTED));
        header.add(Box.createVerticalStrut(4));
        header.add(Theme.label(ending.title(), Font.BOLD, 26, Color.WHITE));
        header.add(Box.createVerticalStrut(4));
        header.add(Theme.label(team.name() + " · " + placement, Font.PLAIN, 14, Theme.ON_DARK_MUTED));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Theme.CARD);
        body.setBorder(BorderFactory.createEmptyBorder(20, 28, 12, 28));
        body.add(Theme.label("<html><div style='width:380px'>" + ending.description() + "</div></html>",
                Font.PLAIN, 14, Theme.TEXT));
        body.add(Box.createVerticalStrut(14));
        body.add(Theme.label(String.format("%d승 %d무 %d패 · 득실 %+d · 승점 %d",
                team.won(), team.drawn(), team.lost(), team.goalDifference(), team.points()), Font.BOLD, 14, Theme.TEXT));
        body.add(Box.createVerticalStrut(8));
        Player star = ending.star();
        String starLine = ending.starStays()
                ? String.format("에이스 %s(능력 %d)는 잔류를 선언했습니다.", star.name(), star.rating())
                : String.format("선수단 분위기가 나빠 에이스 %s(능력 %d)가 이적을 요청했습니다.", star.name(), star.rating());
        body.add(Theme.label(starLine, Font.PLAIN, 13, ending.starStays() ? Theme.GOOD : Theme.BAD));
        body.add(Box.createVerticalStrut(4));
        body.add(Theme.label(String.format("팀 득점왕: %s (%d골)", topScorer.name(), topScorer.goals()),
                Font.PLAIN, 13, Theme.MUTED));
        if (!career.isEmpty()) {
            body.add(Box.createVerticalStrut(14));
            body.add(Theme.label("감독 커리어", Font.BOLD, 14, Theme.TEXT));
            for (String line : career) {
                body.add(Box.createVerticalStrut(3));
                body.add(Theme.label(line, Font.PLAIN, 13, line.contains("우승") ? Theme.GOOD : Theme.MUTED));
            }
        }

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setBackground(Theme.CARD);
        buttons.setBorder(BorderFactory.createEmptyBorder(8, 28, 20, 28));
        JButton close = Theme.secondaryButton("결과 보기");
        JButton newGame = Theme.primaryButton("새 게임");
        close.addActionListener(e -> dialog.dispose());
        newGame.addActionListener(e -> {
            again[0] = true;
            dialog.dispose();
        });
        buttons.add(close);
        buttons.add(newGame);

        JPanel root = new JPanel(new BorderLayout());
        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return again[0];
    }
}
