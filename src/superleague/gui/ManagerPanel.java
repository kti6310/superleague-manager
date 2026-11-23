package superleague.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import superleague.model.ManagerStyle;

class ManagerPanel extends JPanel {
    private static final int MAX_NAME = 12;
    private final JTextField nameField = new JTextField(16);
    private final List<Widgets.RoundPanel> cards = new ArrayList<>();
    private final JButton next = Theme.primaryButton("다음: 구단 선택  ▶");
    private ManagerStyle selected;

    ManagerPanel(BiConsumer<String, ManagerStyle> onDone, Runnable onBack) {
        super(new BorderLayout());
        setBackground(Theme.BG);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(Theme.NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(28, 32, 24, 32));
        header.add(Theme.label("감독 프로필 만들기", Font.BOLD, 28, Color.WHITE));
        header.add(Box.createVerticalStrut(6));
        header.add(Theme.label("이름과 코칭 스타일을 정하세요. 코칭 스타일은 5시즌 내내 팀에 영향을 줍니다.",
                Font.PLAIN, 14, Theme.ON_DARK_MUTED));
        add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setBackground(Theme.BG);
        body.setBorder(BorderFactory.createEmptyBorder(24, 32, 20, 32));

        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        nameRow.setOpaque(false);
        nameRow.add(Theme.label("감독 이름", Font.BOLD, 15, Theme.TEXT));
        nameField.setFont(Theme.font(Font.PLAIN, 15));
        Theme.styleInput(nameField);
        nameField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            public void removeUpdate(DocumentEvent e) {
                update();
            }

            public void changedUpdate(DocumentEvent e) {
                update();
            }
        });
        nameRow.add(nameField);
        nameRow.add(Theme.label("최대 " + MAX_NAME + "자", Font.PLAIN, 12, Theme.MUTED));
        body.add(nameRow, BorderLayout.NORTH);

        JPanel styles = new JPanel(new BorderLayout(0, 10));
        styles.setOpaque(false);
        styles.add(Theme.label("코칭 스타일", Font.BOLD, 15, Theme.TEXT), BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(0, 3, 12, 12));
        grid.setOpaque(false);
        for (ManagerStyle s : ManagerStyle.values()) {
            grid.add(styleCard(s));
        }
        styles.add(grid, BorderLayout.CENTER);
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(styles, BorderLayout.NORTH);
        body.add(holder, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Theme.CARD);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(14, 32, 14, 32)));
        JButton back = Theme.secondaryButton("◀  처음으로");
        back.addActionListener(e -> onBack.run());
        footer.add(back, BorderLayout.WEST);
        next.setEnabled(false);
        next.addActionListener(e -> onDone.accept(name(), selected));
        footer.add(next, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
    }

    private String name() {
        String n = nameField.getText().strip();
        return n.length() > MAX_NAME ? n.substring(0, MAX_NAME) : n;
    }

    private void update() {
        next.setEnabled(!name().isEmpty() && selected != null);
    }

    private Widgets.RoundPanel styleCard(ManagerStyle style) {
        Widgets.RoundPanel card = new Widgets.RoundPanel(new BorderLayout(0, 8), 12);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        card.setBackground(Theme.CARD);
        card.setLine(Theme.BORDER, 1);
        card.add(Theme.label(style.label, Font.BOLD, 18, Theme.TEXT), BorderLayout.NORTH);
        card.add(Theme.label("<html>" + style.motto + "</html>", Font.PLAIN, 13, Theme.MUTED), BorderLayout.CENTER);
        JLabel effect = Theme.label("<html>" + style.effect + "</html>", Font.BOLD, 13, Theme.ACCENT);
        card.add(effect, BorderLayout.SOUTH);
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (selected != style) {
                    card.setLine(Theme.ACCENT_BRIGHT, 1);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (selected != style) {
                    card.setLine(Theme.BORDER, 1);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                selected = style;
                for (int i = 0; i < cards.size(); i++) {
                    boolean on = ManagerStyle.values()[i] == style;
                    cards.get(i).setBackground(on ? Theme.ACCENT_SOFT : Theme.CARD);
                    cards.get(i).setLine(on ? Theme.ACCENT : Theme.BORDER, on ? 2 : 1);
                }
                update();
            }
        });
        cards.add(card);
        return card;
    }
}
