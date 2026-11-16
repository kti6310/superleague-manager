package superleague.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import superleague.model.Attributes;
import superleague.model.Player;
import superleague.model.Position;
import superleague.model.Role;
import superleague.model.Team;
import superleague.model.TeamTraining;

final class TrainingPanel extends JPanel {
    private static final String[] AFFECTS = {"가속도·주력·민첩성", "골 결정력·중거리 슛·침착성",
            "패스·시야·크로스·판단력", "드리블·개인기·퍼스트 터치", "태클·일대일 마크·수비 위치 선정", "몸싸움·지구력·점프 거리"};
    private final Team team;
    private final Runnable onChange;
    private final java.util.Map<TeamTraining, javax.swing.JRadioButton> options = new java.util.EnumMap<>(TeamTraining.class);
    private final JLabel teamNote = Theme.label("", Font.BOLD, 13, Theme.ACCENT);
    private final DefaultTableModel model = Tables.model("포지션", "선수", "나이", "능력", "개인 훈련", "진행", "컨디션");
    private final JTable table;
    private List<Player> rows = List.of();
    private boolean updating;
    private boolean editable = true;

    private final superleague.game.Season season;
    private final DefaultTableModel calendarModel = Tables.model("날짜", "일정", "활동", "효과");
    private final List<java.time.LocalDate> calendarDays = new java.util.ArrayList<>();
    private final javax.swing.JButton nextDay = Theme.primaryButton("다음 날  ▶");
    private final javax.swing.JButton toMatch = Theme.secondaryButton("경기 당일까지  ▶▶");

    TrainingPanel(Team team, superleague.game.Season season, Runnable onChange,
                  java.util.function.Consumer<Boolean> advance, javax.swing.JComponent intensity) {
        super(new BorderLayout(0, 12));
        this.team = team;
        this.onChange = onChange;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));

        java.awt.CardLayout cards = new java.awt.CardLayout();
        JPanel pages = new JPanel(cards);
        pages.setOpaque(false);
        this.season = season;
        Widgets.Segmented switcher = new Widgets.Segmented(new String[]{"일정", "팀 훈련", "개인 훈련"},
                i -> cards.show(pages, i == 0 ? "calendar" : i == 1 ? "team" : "individual")) {
            @Override
            public java.awt.Dimension getPreferredSize() {
                return new java.awt.Dimension(360, 40);
            }
        };
        switcher.setSelectedIndex(0);
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        top.setOpaque(false);
        top.add(switcher);
        add(top, BorderLayout.NORTH);
        add(pages, BorderLayout.CENTER);

        JPanel teamPage = new JPanel();
        teamPage.setLayout(new javax.swing.BoxLayout(teamPage, javax.swing.BoxLayout.Y_AXIS));
        teamPage.setBackground(Theme.BG);
        JPanel current = new JPanel(new BorderLayout(0, 4));
        current.setOpaque(false);
        current.add(Theme.label("이번 주 팀 훈련", Font.BOLD, 14, Theme.TEXT), BorderLayout.NORTH);
        current.add(teamNote, BorderLayout.CENTER);
        teamPage.add(block(current));
        teamPage.add(block(Theme.card("훈련 강도", intensity)));
        javax.swing.ButtonGroup group = new javax.swing.ButtonGroup();
        JPanel grid = new JPanel(new java.awt.GridLayout(0, 2, 12, 12));
        grid.setOpaque(false);
        for (String category : TeamTraining.CATEGORIES) {
            JPanel list = new JPanel();
            list.setLayout(new javax.swing.BoxLayout(list, javax.swing.BoxLayout.Y_AXIS));
            list.setOpaque(false);
            for (TeamTraining t : TeamTraining.values()) {
                if (!t.category.equals(category)) {
                    continue;
                }
                javax.swing.JRadioButton radio = new javax.swing.JRadioButton(String.format(
                        "<html><b>%s</b><br><font color='#64748B'>%s</font><br><font color='#2563EB'>%s</font></html>",
                        t.label, t.description, t.effectText()));
                radio.setOpaque(false);
                radio.setFont(Theme.font(Font.PLAIN, 12));
                radio.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
                radio.addActionListener(e -> {
                    if (!updating) {
                        team.setTeamTraining(t);
                        onChange.run();
                    }
                });
                group.add(radio);
                options.put(t, radio);
                list.add(radio);
                list.add(javax.swing.Box.createVerticalStrut(6));
            }
            grid.add(Theme.card(category, list));
        }
        teamPage.add(block(grid));
        javax.swing.JScrollPane teamScroll = new javax.swing.JScrollPane(teamPage);
        teamScroll.setBorder(BorderFactory.createEmptyBorder());
        teamScroll.getVerticalScrollBar().setUnitIncrement(16);
        JPanel calendar = new JPanel(new BorderLayout(0, 8));
        calendar.setOpaque(false);
        JPanel calendarTop = new JPanel(new BorderLayout(0, 6));
        calendarTop.setOpaque(false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        nextDay.addActionListener(e -> advance.accept(false));
        toMatch.addActionListener(e -> advance.accept(true));
        buttons.add(nextDay);
        buttons.add(toMatch);
        calendarTop.add(buttons, BorderLayout.NORTH);
        calendarTop.add(Theme.label("<html>하루씩 훈련·휴식을 정합니다. 기본 계획은 경기 다음 날 회복, 경기 전날 가벼운 훈련, 일요일 휴식, "
                + "나머지 날 훈련입니다. '활동' 칸을 눌러 바꿀 수 있습니다. 훈련하는 날에는 이번 주 팀 훈련과 훈련 강도가 적용됩니다.</html>",
                Font.PLAIN, 12, Theme.MUTED), BorderLayout.CENTER);
        calendar.add(calendarTop, BorderLayout.NORTH);
        JTable calendarTable = Tables.table(calendarModel, row -> row == 0, 1, 2, 3);
        Tables.widths(calendarTable, 130, 220, 110, 420);
        calendarTable.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        calendarTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = calendarTable.rowAtPoint(e.getPoint());
                if (row < 0 || row >= calendarDays.size() || !editable) {
                    return;
                }
                java.time.LocalDate day = calendarDays.get(row);
                if (season.roundOn(day) >= 0) {
                    return;
                }
                JPopupMenu menu = new JPopupMenu();
                for (superleague.model.DayActivity a : superleague.model.DayActivity.values()) {
                    JMenuItem item = new JMenuItem(String.format("<html><b>%s%s</b><br><font color='#64748B'>%s</font></html>",
                            season.activityOn(day) == a ? "✓ " : "", a.label, a.description));
                    item.addActionListener(ev -> {
                        season.setActivity(day, a);
                        onChange.run();
                    });
                    menu.add(item);
                }
                java.awt.Rectangle r = calendarTable.getCellRect(row, 2, true);
                menu.show(calendarTable, r.x, r.y + r.height);
            }
        });
        calendar.add(Tables.scroll(calendarTable), BorderLayout.CENTER);
        pages.add(calendar, "calendar");
        pages.add(teamScroll, "team");

        JPanel individual = new JPanel(new BorderLayout(0, 8));
        individual.setOpaque(false);
        individual.add(Theme.label("<html>선수의 '개인 훈련' 칸을 눌러 집중할 능력이나 새로 익힐 포지션을 고르세요. "
                + "능력은 라운드마다 확률로 +2 (젊을수록 잘 오름), 포지션은 " + Player.ROUNDS_TO_LEARN
                + "점이 모이면 보조 포지션이 됩니다 (23세 이하 라운드당 2점, 그 외 1점). 선수 이름을 누르면 선수 카드가 뜹니다.</html>",
                Font.PLAIN, 12, Theme.MUTED), BorderLayout.NORTH);
        pages.add(individual, "individual");

        table = Tables.table(model, row -> false, 1, 4);
        Tables.widths(table, 90, 160, 44, 50, 130, 110, 56);
        table.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row < 0 || row >= rows.size()) {
                    return;
                }
                if (col == 4 && editable) {
                    java.awt.Rectangle r = table.getCellRect(row, col, true);
                    openMenu(rows.get(row), r.x, r.y + r.height);
                } else {
                    PlayerCard.show(table, rows.get(row), team.name());
                }
            }
        });
        individual.add(Tables.scroll(table), BorderLayout.CENTER);
    }

    private void refreshCalendar() {
        calendarModel.setRowCount(0);
        calendarDays.clear();
        boolean running = !season.isFinished();
        nextDay.setEnabled(running && !season.isMatchDay());
        toMatch.setEnabled(running && !season.isMatchDay());
        nextDay.setText(season.isMatchDay() ? "오늘은 경기일" : "다음 날  ▶");
        if (!running) {
            return;
        }
        java.time.LocalDate day = season.today();
        int matchesShown = 0;
        for (int i = 0; i < 60 && matchesShown < 2; i++, day = day.plusDays(1)) {
            int round = season.roundOn(day);
            String event;
            String activity;
            String effect;
            if (round >= 0) {
                superleague.sim.Fixture f = season.userSchedule().get(round);
                event = String.format("%d라운드 vs %s (%s)", round + 1, f.opponentOf(team).name(), f.home() == team ? "홈" : "원정");
                activity = "경기";
                effect = "";
                matchesShown++;
            } else {
                superleague.model.DayActivity a = season.activityOn(day);
                event = i == 0 ? "오늘" : "";
                activity = a.label;
                effect = a == superleague.model.DayActivity.TRAINING
                        ? team.teamTraining().label + " · " + team.intensity().label() : a.description;
            }
            calendarDays.add(day);
            calendarModel.addRow(new Object[]{superleague.game.Season.formatDate(day), event, activity, effect});
        }
    }

    private static javax.swing.JComponent block(javax.swing.JComponent c) {
        JPanel wrap = new JPanel(new BorderLayout()) {
            @Override
            public java.awt.Dimension getMaximumSize() {
                return new java.awt.Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        wrap.setOpaque(false);
        wrap.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        wrap.add(c, BorderLayout.CENTER);
        wrap.setAlignmentX(LEFT_ALIGNMENT);
        return wrap;
    }

    void refresh(boolean editable) {
        this.editable = editable;
        refreshCalendar();
        updating = true;
        options.forEach((t, radio) -> {
            radio.setSelected(t == team.teamTraining());
            radio.setEnabled(editable);
        });
        updating = false;
        TeamTraining t = team.teamTraining();
        teamNote.setText("[" + t.category + "] " + t.label + "  ·  " + t.effectText());
        rows = team.squad().stream()
                .sorted(Comparator.comparing(Player::position).thenComparing(Player::rating, Comparator.reverseOrder()))
                .toList();
        model.setRowCount(0);
        for (Player p : rows) {
            String progress = p.learning() != null
                    ? p.learningProgress() + " / " + Player.ROUNDS_TO_LEARN
                    : p.trainedGain() > 0 ? "이번 시즌 +" + p.trainedGain() : "";
            model.addRow(new Object[]{p.positions(), p.name(), p.age(), p.rating(), p.trainingLabel(), progress,
                    p.condition()});
        }
    }

    private void openMenu(Player p, int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem none = new JMenuItem("없음");
        none.addActionListener(e -> apply(() -> p.setFocus(-1)));
        menu.add(none);
        menu.addSeparator();
        for (int i = 0; i < Attributes.NAMES.length; i++) {
            int index = i;
            JMenuItem item = new JMenuItem(String.format("%s%s 집중  →  %s", p.focus() == i ? "✓ " : "",
                    Attributes.NAMES[i], AFFECTS[i]));
            item.addActionListener(e -> apply(() -> p.setFocus(index)));
            menu.add(item);
        }
        JMenu learn = new JMenu("새 포지션 익히기");
        for (Role r : Role.values()) {
            boolean keeper = r == Role.GK;
            if (r == p.role() || p.secondary().contains(r) || keeper != (p.position() == Position.GK)) {
                continue;
            }
            JMenuItem item = new JMenuItem((p.learning() == r ? "✓ " : "") + r.name() + "  " + r.description());
            item.addActionListener(e -> apply(() -> p.setLearning(r)));
            learn.add(item);
        }
        if (learn.getItemCount() > 0) {
            menu.addSeparator();
            menu.add(learn);
        }
        menu.show(table, x, y);
    }

    private void apply(Runnable change) {
        change.run();
        onChange.run();
    }
}
