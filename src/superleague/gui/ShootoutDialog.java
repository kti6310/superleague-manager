package superleague.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import superleague.game.Shootout;

final class ShootoutDialog extends JDialog {
    private final Shootout shootout;
    private final JLabel score = Theme.label("0 : 0", Font.BOLD, 40, Color.WHITE);
    private final JLabel status = Theme.label("승부차기 준비", Font.PLAIN, 14, Theme.ON_DARK_MUTED);
    private final JPanel feed = new JPanel();
    private final JScrollPane scroll;
    private final JButton skip = Theme.secondaryButton("결과 바로 보기");
    private final JButton close = Theme.primaryButton("닫기");
    private final Timer timer;
    private int index;
    private boolean waitingResult;

    private ShootoutDialog(Component parent, Shootout shootout) {
        super(SwingUtilities.getWindowAncestor(parent), "승부차기", ModalityType.APPLICATION_MODAL);
        this.shootout = shootout;

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        header.add(Theme.label(shootout.first.name(), Font.BOLD, 18, Color.WHITE), BorderLayout.WEST);
        JPanel mid = new JPanel();
        mid.setOpaque(false);
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        score.setAlignmentX(CENTER_ALIGNMENT);
        status.setAlignmentX(CENTER_ALIGNMENT);
        mid.add(score);
        mid.add(status);
        header.add(mid, BorderLayout.CENTER);
        header.add(Theme.label(shootout.second.name(), Font.BOLD, 18, Color.WHITE), BorderLayout.EAST);

        feed.setLayout(new BoxLayout(feed, BoxLayout.Y_AXIS));
        feed.setBackground(Theme.CARD);
        feed.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JPanel holder = new JPanel(new BorderLayout());
        holder.setBackground(Theme.CARD);
        holder.add(feed, BorderLayout.NORTH);
        scroll = new JScrollPane(holder);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        skip.addActionListener(e -> {
            while (index < shootout.kicks().size()) {
                if (!waitingResult) {
                    announce();
                }
                reveal();
            }
            done();
        });
        close.addActionListener(e -> dispose());
        close.setEnabled(false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttons.setBackground(Theme.BG);
        buttons.add(skip);
        buttons.add(close);

        JPanel root = new JPanel(new BorderLayout());
        root.add(header, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        setSize(640, 560);
        setLocationRelativeTo(parent);

        timer = new Timer(1100, e -> {
            if (index >= shootout.kicks().size()) {
                done();
            } else if (waitingResult) {
                reveal();
            } else {
                announce();
            }
        });
        timer.setInitialDelay(800);
        timer.start();
    }

    static void show(Component parent, Shootout shootout) {
        if (shootout != null) {
            new ShootoutDialog(parent, shootout).setVisible(true);
        }
    }

    @Override
    public void dispose() {
        timer.stop();
        super.dispose();
    }

    private void announce() {
        Shootout.Kick k = shootout.kicks().get(index);
        status.setText(k.team().name() + " · " + k.taker().name() + " 키커 준비...");
        add(String.format("%d번째 키커 · %s %s, 공을 내려놓습니다...", index / 2 + 1, k.team().name(), k.taker().name()),
                Theme.MUTED, Font.PLAIN);
        waitingResult = true;
    }

    private void reveal() {
        Shootout.Kick k = shootout.kicks().get(index++);
        add("   " + k.text(), k.scored() ? Theme.GOOD : Theme.BAD, Font.BOLD);
        score.setText(k.firstScore() + " : " + k.secondScore());
        status.setText(k.scored() ? "골!" : "실축!");
        waitingResult = false;
    }

    private void done() {
        timer.stop();
        status.setText(shootout.winner().name() + " 승리 (" + shootout.score() + ")");
        add(shootout.winner().name() + ", 승부차기 " + shootout.score() + "로 승리!", Theme.ACCENT, Font.BOLD);
        skip.setEnabled(false);
        close.setEnabled(true);
        getRootPane().setDefaultButton(close);
    }

    private void add(String text, Color color, int style) {
        JLabel l = Theme.label(text, style, 14, color);
        l.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        feed.add(l);
        feed.revalidate();
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum()));
    }
}
