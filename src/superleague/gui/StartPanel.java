package superleague.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import superleague.data.TeamData;
import superleague.model.Team;
import superleague.model.Tier;

class StartPanel extends JPanel {
    private final List<JPanel> cards = new ArrayList<>();
    private final List<Team> teams = TeamData.createTeams();
    private final JButton startButton = Theme.primaryButton("감독 부임");
    private final JLabel selectionLabel = Theme.label("구단을 선택하세요.", Font.PLAIN, 13, Theme.MUTED);
    private final JTextField seedField = new JTextField(10);
    private int selected = -1;
    private int hovered = -1;
    private final JPanel colorRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    private Kits.Kit chosenKit;
    private Swatch chosenSwatch;

    /** Round kit swatch for the team-colour picker. */
    private final class Swatch extends JComponent {
        private final Kits.Kit kit;
        private boolean hover;

        Swatch(Kits.Kit kit, String tip, java.util.function.Consumer<Swatch> onPick) {
            this.kit = kit;
            setPreferredSize(new Dimension(30, 30));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(tip);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onPick.accept(Swatch.this);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Widgets.smooth(g);
            int c = getWidth() / 2;
            if (chosenSwatch == this) {
                g2.setColor(Theme.ACCENT);
                g2.setStroke(new java.awt.BasicStroke(2.5f));
                g2.drawOval(1, 1, getWidth() - 3, getHeight() - 3);
            } else if (hover) {
                g2.setColor(Theme.BORDER_STRONG);
                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.drawOval(1, 1, getWidth() - 3, getHeight() - 3);
            }
            PitchView.drawKitDot(g2, c, c - 1, 10, kit);
            g2.dispose();
        }
    }

    private void buildColorRow(Team team) {
        colorRow.removeAll();
        colorRow.add(Theme.label("팀 색깔", Font.BOLD, 13, Theme.TEXT));
        java.util.function.Consumer<Swatch> pick = s -> {
            chosenSwatch = s;
            chosenKit = s.kit == Kits.preset(team.name()) ? null : s.kit;
            colorRow.repaint();
        };
        Swatch def = new Swatch(Kits.preset(team.name()), "구단 기본 유니폼", s -> {
            chosenSwatch = s;
            chosenKit = null;
            colorRow.repaint();
        });
        colorRow.add(def);
        colorRow.add(Theme.label("|", Font.PLAIN, 13, Theme.BORDER_STRONG));
        for (int rgb : Kits.PALETTE) {
            colorRow.add(new Swatch(Kits.solid(new Color(rgb)), "이 색으로 유니폼 바꾸기", pick));
        }
        chosenSwatch = def;
        chosenKit = null;
        colorRow.setVisible(true);
        colorRow.revalidate();
        colorRow.repaint();
    }

    StartPanel(BiConsumer<Integer, Long> onStart) {
        super(new BorderLayout());
        setBackground(Theme.BG);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(Theme.NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(28, 32, 24, 32));
        JLabel title = Theme.label("SUPER LEAGUE MANAGER", Font.BOLD, 28, Color.WHITE);
        JLabel subtitle = Theme.label("슈퍼리그 18개 구단 중 한 팀을 맡아 5시즌 동안 지휘하세요.",
                Font.PLAIN, 14, Theme.ON_DARK_MUTED);
        header.add(title);
        header.add(javax.swing.Box.createVerticalStrut(6));
        header.add(subtitle);
        header.add(javax.swing.Box.createVerticalStrut(10));
        java.nio.file.Path dataPath = superleague.data.SquadLoader.resolve();
        boolean hasData = java.nio.file.Files.exists(dataPath);
        header.add(Theme.label(hasData
                        ? "선수 데이터: data/players.csv 를 불러옵니다. 선수를 넣지 않은 구단은 자동 생성됩니다."
                        : "선수 데이터: data/players.csv 가 없어 모든 선수를 자동 생성합니다.",
                Font.PLAIN, 12, hasData ? new Color(0x8FD3A8) : Theme.ON_DARK_MUTED));
        add(header, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 3, 12, 12));
        grid.setBackground(Theme.BG);
        grid.setBorder(BorderFactory.createEmptyBorder(24, 32, 20, 32));
        for (int i = 0; i < teams.size(); i++) {
            JPanel card = teamCard(teams.get(i), i);
            cards.add(card);
            grid.add(card);
        }
        add(grid, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Theme.CARD);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(14, 32, 14, 32)));
        footer.add(selectionLabel, BorderLayout.WEST);
        colorRow.setOpaque(false);
        colorRow.setVisible(false);
        colorRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        footer.add(colorRow, BorderLayout.NORTH);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setBackground(Theme.CARD);
        right.add(Theme.label("시드 (비우면 무작위)", Font.PLAIN, 12, Theme.MUTED));
        seedField.setFont(Theme.font(Font.PLAIN, 13));
        Theme.styleInput(seedField);
        right.add(seedField);
        startButton.setEnabled(false);
        startButton.addActionListener(e -> {
            // only the user's club can be recoloured; null = keep the club's own kit
            Kits.setUserKit(teams.get(selected).name(), chosenKit);
            onStart.accept(selected, parseSeed());
        });
        right.add(startButton);
        footer.add(right, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
    }

    private Long parseSeed() {
        String text = seedField.getText().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return (long) text.hashCode();
        }
    }

    private JPanel teamCard(Team team, int index) {
        Widgets.RoundPanel card = new Widgets.RoundPanel(new BorderLayout(12, 2), 12);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        card.setFocusable(true);

        Color tierColor = Theme.tierColor(team.tier());
        JComponent badge = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Widgets.smooth(g);
                g2.setColor(tierColor);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(Color.WHITE);
                g2.setFont(Theme.font(Font.BOLD, 16));
                Widgets.drawCentered(g2, team.tier().name(), getWidth(), getHeight());
                g2.dispose();
            }
        };
        badge.setPreferredSize(new Dimension(40, 40));
        JPanel badgeWrap = new JPanel(new java.awt.GridBagLayout());
        badgeWrap.setOpaque(false);
        badgeWrap.add(badge);
        card.add(badgeWrap, BorderLayout.WEST);

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);
        text.add(Theme.label(team.name(), Font.BOLD, 14, Theme.TEXT));
        text.add(Theme.label(String.format("%s · 목표 %s", team.country(), team.tier().targetLabel()),
                Font.PLAIN, 12, Theme.MUTED));
        card.add(text, BorderLayout.CENTER);
        Kits.Kit kit = Kits.preset(team.name());
        JComponent kitDot = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Widgets.smooth(g);
                PitchView.drawKitDot(g2, getWidth() / 2, getHeight() / 2 - 1, 9, kit);
                g2.dispose();
            }
        };
        kitDot.setPreferredSize(new Dimension(24, 24));
        kitDot.setToolTipText("구단 유니폼 색");
        JPanel kitWrap = new JPanel(new java.awt.GridBagLayout());
        kitWrap.setOpaque(false);
        kitWrap.add(kitDot);
        card.add(kitWrap, BorderLayout.EAST);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                card.requestFocusInWindow();
                select(index);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = index;
                paintCard(index);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = -1;
                paintCard(index);
            }
        });
        card.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
                    select(index);
                }
            }
        });
        card.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                paintCard(index);
            }

            @Override
            public void focusLost(FocusEvent e) {
                paintCard(index);
            }
        });
        return card;
    }

    private void paintCard(int index) {
        Widgets.RoundPanel card = (Widgets.RoundPanel) cards.get(index);
        if (index == selected) {
            card.setBackground(Theme.ACCENT_SOFT);
            card.setLine(Theme.ACCENT, 2);
        } else if (index == hovered || card.isFocusOwner()) {
            card.setBackground(Theme.CARD);
            card.setLine(Theme.ACCENT_BRIGHT, 1);
        } else {
            card.setBackground(Theme.CARD);
            card.setLine(Theme.BORDER, 1);
        }
    }

    private void select(int index) {
        int previous = selected;
        selected = index;
        if (previous >= 0) {
            paintCard(previous);
        }
        paintCard(index);
        Team team = teams.get(index);
        Tier tier = team.tier();
        selectionLabel.setText(String.format("<html><b>%s</b> 감독으로 부임 · %s등급 · 운영진 목표 %s</html>",
                team.name(), tier.name(), tier.targetLabel()));
        selectionLabel.setForeground(Theme.TEXT);
        startButton.setEnabled(true);
        if (previous != index) {
            buildColorRow(team);
        }
    }
}
