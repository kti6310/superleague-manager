package superleague.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

final class Dialogs {
    enum Tone {
        INFO, GOOD, WARN
    }

    record Option(String label, String detail) {
    }

    private Dialogs() {
    }

    static int show(Component parent, String title, String heading, List<String> lines, Tone tone,
                    List<Option> options, String... buttons) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, JDialog.ModalityType.APPLICATION_MODAL);
        int[] result = {-1};

        Color toneColor = switch (tone) {
            case GOOD -> Theme.GOOD;
            case WARN -> Theme.WARN;
            default -> Theme.ACCENT;
        };

        JPanel head = new JPanel();
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setBackground(Theme.NAVY);
        head.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, toneColor),
                BorderFactory.createEmptyBorder(18, 24, 16, 24)));
        head.add(Theme.label(title.toUpperCase(), Font.BOLD, 12, Theme.ON_DARK_MUTED));
        head.add(Box.createVerticalStrut(4));
        head.add(Theme.label(heading, Font.BOLD, 19, Color.WHITE));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Theme.CARD);
        body.setBorder(BorderFactory.createEmptyBorder(18, 24, 8, 24));
        for (String line : lines) {
            JLabel l = Theme.label("<html><div style='width:400px'>" + escape(line) + "</div></html>",
                    Font.PLAIN, 14, Theme.TEXT);
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(l);
            body.add(Box.createVerticalStrut(8));
        }

        if (options != null && !options.isEmpty()) {
            body.add(Box.createVerticalStrut(4));
            for (int i = 0; i < options.size(); i++) {
                int index = i;
                Option o = options.get(i);
                JButton b = optionButton(o);
                b.addActionListener(e -> {
                    result[0] = index;
                    dialog.dispose();
                });
                b.setAlignmentX(Component.LEFT_ALIGNMENT);
                body.add(b);
                body.add(Box.createVerticalStrut(8));
            }
        }

        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        foot.setBackground(Theme.CARD);
        foot.setBorder(BorderFactory.createEmptyBorder(8, 24, 20, 24));
        JButton defaultButton = null;
        for (int i = 0; i < buttons.length; i++) {
            int index = (options == null ? 0 : options.size()) + i;
            boolean primary = i == buttons.length - 1;
            JButton b = primary ? Theme.primaryButton(buttons[i]) : Theme.secondaryButton(buttons[i]);
            b.addActionListener(e -> {
                result[0] = index;
                dialog.dispose();
            });
            foot.add(b);
            if (primary) {
                defaultButton = b;
            }
        }

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.CARD);
        root.setBorder(BorderFactory.createLineBorder(Theme.BORDER_STRONG));
        root.add(head, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        if (buttons.length > 0) {
            root.add(foot, BorderLayout.SOUTH);
        }
        root.registerKeyboardAction(e -> dialog.dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        dialog.setContentPane(root);
        if (defaultButton != null) {
            dialog.getRootPane().setDefaultButton(defaultButton);
        }
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return result[0];
    }

    static void showScroll(Component parent, String title, String heading, String text) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, JDialog.ModalityType.APPLICATION_MODAL);
        JPanel head = new JPanel();
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setBackground(Theme.NAVY);
        head.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, Theme.WARN),
                BorderFactory.createEmptyBorder(18, 24, 16, 24)));
        head.add(Theme.label(title.toUpperCase(), Font.BOLD, 12, Theme.ON_DARK_MUTED));
        head.add(Box.createVerticalStrut(4));
        head.add(Theme.label(heading, Font.BOLD, 19, Color.WHITE));

        javax.swing.JTextArea area = new javax.swing.JTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Theme.font(Font.PLAIN, 13));
        area.setForeground(Theme.TEXT);
        area.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(new Dimension(560, Math.min(320, 60 + text.split("\n").length * 22)));

        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        foot.setBackground(Theme.CARD);
        foot.setBorder(BorderFactory.createEmptyBorder(8, 24, 20, 24));
        JButton ok = Theme.primaryButton("확인");
        ok.addActionListener(e -> dialog.dispose());
        foot.add(ok);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.CARD);
        root.setBorder(BorderFactory.createLineBorder(Theme.BORDER_STRONG));
        root.add(head, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(foot, BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.getRootPane().setDefaultButton(ok);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static JButton optionButton(Option o) {
        JButton b = new JButton() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = Widgets.smooth(g);
                boolean hover = getModel().isRollover();
                g2.setColor(hover ? Theme.ACCENT_SOFT : Theme.CARD);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(hover || isFocusOwner() ? Theme.ACCENT : Theme.BORDER_STRONG);
                g2.setStroke(new java.awt.BasicStroke(hover || isFocusOwner() ? 2f : 1f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
                g2.setFont(Theme.font(Font.BOLD, 14));
                g2.setColor(Theme.TEXT);
                g2.drawString(o.label(), 16, 26);
                g2.setFont(Theme.font(Font.PLAIN, 12));
                g2.setColor(Theme.MUTED);
                g2.drawString(o.detail(), 16, 46);
                g2.dispose();
            }
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setRolloverEnabled(true);
        b.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(420, 60));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        return b;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
