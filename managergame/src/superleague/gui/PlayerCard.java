package superleague.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import superleague.model.Attributes;
import superleague.model.Formation;
import superleague.model.Player;
import superleague.model.Role;

final class PlayerCard extends JDialog {
    private static final Color DIVIDER = new Color(0xD8DCE6);

    private PlayerCard(Component parent, Player p, String teamName) {
        super(SwingUtilities.getWindowAncestor(parent), p.name(), ModalityType.APPLICATION_MODAL);

        JPanel head = new JPanel();
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setBackground(Color.WHITE);
        head.setBorder(BorderFactory.createEmptyBorder(26, 36, 24, 36));

        JPanel title = row();
        JLabel ovr = Theme.label(" " + p.rating() + " ", Font.BOLD, 15, Color.WHITE);
        ovr.setOpaque(true);
        ovr.setBackground(Theme.NAVY);
        ovr.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        title.add(ovr);
        title.add(Box.createHorizontalStrut(12));
        title.add(Theme.label(p.name(), Font.BOLD, 30, Theme.TEXT));
        head.add(title);
        head.add(Box.createVerticalStrut(12));

        JPanel positions = row();
        positions.add(positionRating(p, p.role()));
        for (Role r : p.secondary()) {
            positions.add(Box.createHorizontalStrut(16));
            positions.add(positionRating(p, r));
        }
        head.add(positions);
        head.add(Box.createVerticalStrut(10));

        head.add(infoRow(p.age() + "세", p.height() + "cm", p.weight() + "kg", p.foot().label(),
                "최대 " + p.potential(), "성장 " + (p.growth() > 0 ? "+" + p.growth() : "0")));
        head.add(Box.createVerticalStrut(8));
        head.add(infoRow(teamName, p.nationality().isBlank() ? "국적 미상" : p.nationality(),
                "체력 " + p.condition() + "%", "출전 " + p.appearances() + " · 골 " + p.goals(),
                p.isAvailable() ? "출전 가능" : "부상 " + p.injuredRounds() + "경기", "개인 훈련: " + p.trainingLabel()));

        Attributes.Section[] sections = Attributes.sections(p);
        JPanel grid = new JPanel(new GridLayout(1, sections.length + 1, 18, 0));
        grid.setBackground(new Color(0xFAFAFC));
        grid.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        for (Attributes.Section sec : sections) {
            JPanel col = new JPanel(new GridLayout(15, 1, 0, 2));
            col.setOpaque(false);
            col.add(Theme.label(sec.title(), Font.BOLD, 13, Theme.MUTED));
            for (int i = 0; i < sec.names().length; i++) {
                col.add(statLine(sec.names()[i], String.valueOf(sec.values()[i]), statColor(sec.values()[i]), sec.marks()[i]));
            }
            if (sec.title().equals("신체")) {
                col.add(new JLabel());
                col.add(statLine("신장", p.height() + " cm", Theme.TEXT, 0));
                col.add(statLine("체중", p.weight() + " kg", Theme.TEXT, 0));
            }
            grid.add(col);
        }
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        for (String[] item : Attributes.profile(p)) {
            JLabel k = Theme.label(item[0], Font.PLAIN, 12, Theme.MUTED);
            JLabel v = Theme.label("<html>" + item[1].replace("\n", "<br>") + "</html>", Font.BOLD, 13, Theme.TEXT);
            k.setAlignmentX(LEFT_ALIGNMENT);
            v.setAlignmentX(LEFT_ALIGNMENT);
            info.add(k);
            info.add(Box.createVerticalStrut(2));
            info.add(v);
            info.add(Box.createVerticalStrut(10));
        }
        grid.add(info);

        JButton close = Theme.primaryButton("닫기");
        close.addActionListener(e -> dispose());
        JPanel foot = new JPanel(new BorderLayout());
        foot.setBackground(new Color(0xFAFAFC));
        foot.setBorder(BorderFactory.createEmptyBorder(0, 36, 18, 36));
        foot.add(Theme.label("세부 능력 1~20 · 초록 줄 = 이 포지션 핵심 능력, 파란 줄 = 중요 능력",
                Font.PLAIN, 11, Theme.MUTED), BorderLayout.WEST);
        foot.add(close, BorderLayout.EAST);

        JPanel body = new JPanel(new BorderLayout());
                body.add(grid, BorderLayout.CENTER);
        body.add(foot, BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout());
        root.add(head, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        setContentPane(root);
        getRootPane().setDefaultButton(close);
        pack();
        setSize(Math.max(getWidth(), 1000), getHeight());
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    static void show(Component parent, Player p, String teamName) {
        if (p != null) {
            new PlayerCard(parent, p, teamName).setVisible(true);
        }
    }

    private static final Color KEY_ROW = new Color(0xE3F1E6);
    private static final Color GOOD_ROW = new Color(0xE3EAF6);

    private static Color statColor(int v) {
        return v >= 16 ? new Color(0x15803D) : v >= 11 ? new Color(0xB45309) : new Color(0x6B7280);
    }

    private static JPanel statLine(String name, String value, Color color, int mark) {
        JPanel line = new JPanel(new BorderLayout());
        line.setOpaque(mark > 0);
        line.setBackground(mark == 2 ? KEY_ROW : GOOD_ROW);
        line.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, mark == 2 ? new Color(0x16A34A) : mark == 1 ? new Color(0x2563EB)
                        : new Color(0, 0, 0, 0)),
                BorderFactory.createEmptyBorder(0, 8, 0, 8)));
        line.add(Theme.label(name, Font.PLAIN, 14, Theme.TEXT), BorderLayout.WEST);
        line.add(Theme.label(value, Font.BOLD, 14, color), BorderLayout.EAST);
        return line;
    }

    static Color colorFor(int v) {
        return v >= 180 ? new Color(0xC81E3A) : v >= 150 ? new Color(0x9333EA) : v >= 120 ? new Color(0x4F46E5)
                : new Color(0x6B7280);
    }

    private static JPanel row() {
        JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        r.setOpaque(false);
        r.setAlignmentX(LEFT_ALIGNMENT);
        return r;
    }

    private static JPanel positionRating(Player p, Role role) {
        JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        r.setOpaque(false);
        int value = (int) Math.round(p.rating() * Formation.roleValue(p, role.name()));
        r.add(Theme.label(role.name(), Font.BOLD, 22, Theme.positionColor(role.group())));
        r.add(Theme.label(String.valueOf(value), Font.PLAIN, 22, Theme.TEXT));
        return r;
    }

    private static JPanel infoRow(String... items) {
        JPanel r = row();
        for (int i = 0; i < items.length; i++) {
            if (i > 0) {
                JLabel bar = Theme.label("  |  ", Font.PLAIN, 15, DIVIDER);
                r.add(bar);
            }
            r.add(Theme.label(items[i], Font.PLAIN, 16, Theme.MUTED));
        }
        return r;
    }
}
