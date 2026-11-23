package superleague.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import superleague.game.GroupDraw;
import superleague.game.Season;
import superleague.model.Team;

final class DrawDialog extends JDialog {
    private static final Color SLOT = new Color(0xF1F5F9);
    private static final Color PICK = new Color(0xFEF3C7);
    private final GroupDraw draw;
    private final Team userTeam;
    private final JLabel[] potRows;
    private final JLabel[] slotsA;
    private final JLabel[] slotsB;
    private final JLabel status = Theme.label(" ", Font.PLAIN, 13, Theme.MUTED);
    private final JButton skip = Theme.secondaryButton("결과 바로 보기");
    private final JButton close = Theme.primaryButton("시즌 시작");
    private final Timer timer;
    private int revealed;

    private DrawDialog(Component parent, Season season) {
        super(SwingUtilities.getWindowAncestor(parent), "조 추첨식", ModalityType.APPLICATION_MODAL);
        draw = season.draw();
        userTeam = season.userTeam();
        int n = draw.pots().size();
        potRows = new JLabel[n];
        slotsA = new JLabel[n];
        slotsB = new JLabel[n];

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(Theme.NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(20, 28, 18, 28));
        header.add(Theme.label(season.seasonLabel() + " 슈퍼리그 조 추첨식", Font.BOLD, 24, Color.WHITE));
        header.add(Box.createVerticalStrut(6));
        header.add(Theme.label("전력이 비슷한 두 팀씩 9개 포트로 묶고, 포트마다 한 팀은 A조, 다른 한 팀은 B조로 추첨합니다.",
                Font.PLAIN, 13, Theme.ON_DARK_MUTED));

        JPanel pots = column("추첨 포트 (전력 순)");
        for (int i = 0; i < n; i++) {
            Team[] pot = draw.pots().get(i);
            potRows[i] = slot(String.format("포트 %d   %s · %s", i + 1, pot[0].name(), pot[1].name()));
            pots.add(potRows[i]);
        }
        JPanel a = column("A조");
        JPanel b = column("B조");
        for (int i = 0; i < n; i++) {
            slotsA[i] = slot("?");
            slotsB[i] = slot("?");
            a.add(slotsA[i]);
            b.add(slotsB[i]);
        }
        JPanel board = new JPanel(new GridLayout(1, 3, 16, 0));
        board.setBackground(Theme.BG);
        board.setBorder(BorderFactory.createEmptyBorder(18, 24, 12, 24));
        board.add(pots);
        board.add(a);
        board.add(b);

        skip.addActionListener(e -> {
            while (revealed < n) {
                revealNext();
            }
        });
        close.addActionListener(e -> dispose());
        close.setEnabled(false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(skip);
        buttons.add(close);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Theme.CARD);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)));
        footer.add(status, BorderLayout.WEST);
        footer.add(buttons, BorderLayout.EAST);

        JPanel root = new JPanel(new BorderLayout());
        root.add(header, BorderLayout.NORTH);
        root.add(board, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
        setSize(980, 640);
        setLocationRelativeTo(parent);

        timer = new Timer(800, e -> revealNext());
        timer.setInitialDelay(700);
        timer.start();
    }

    @Override
    public void dispose() {
        timer.stop();
        super.dispose();
    }

    static void show(Component parent, Season season) {
        new DrawDialog(parent, season).setVisible(true);
    }

    private void revealNext() {
        int n = potRows.length;
        if (revealed >= n) {
            return;
        }
        if (revealed > 0) {
            potRows[revealed - 1].setBackground(SLOT);
        }
        int i = revealed++;
        potRows[i].setBackground(PICK);
        fill(slotsA[i], draw.groupA().get(i));
        fill(slotsB[i], draw.groupB().get(i));
        status.setText(String.format("포트 %d: %s → A조, %s → B조", i + 1, draw.groupA().get(i).name(), draw.groupB().get(i).name()));
        if (revealed == n) {
            timer.stop();
            potRows[i].setBackground(SLOT);
            String mine = draw.groupA().contains(userTeam) ? "A조" : "B조";
            status.setText(String.format("추첨 완료 · 우리 팀은 %s · 평균 전력 A조 %.1f / B조 %.1f",
                    mine, draw.averageA(), draw.averageB()));
            skip.setEnabled(false);
            close.setEnabled(true);
            getRootPane().setDefaultButton(close);
        }
    }

    private void fill(JLabel slot, Team team) {
        slot.setText(team.name() + "  (" + team.tier().name() + ")");
        slot.setForeground(Theme.TEXT);
        if (team == userTeam) {
            slot.setBackground(Theme.ACCENT_SOFT);
            slot.setFont(Theme.font(Font.BOLD, 14));
        }
    }

    private static JPanel column(String title) {
        JPanel col = new JPanel(new GridLayout(0, 1, 0, 6));
        col.setOpaque(false);
        JLabel t = Theme.label(title, Font.BOLD, 15, Theme.TEXT);
        col.add(t);
        return col;
    }

    private static JLabel slot(String text) {
        JLabel l = Theme.label(text, Font.PLAIN, 14, Theme.MUTED);
        l.setOpaque(true);
        l.setBackground(SLOT);
        l.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        l.setPreferredSize(new Dimension(260, 36));
        return l;
    }
}
