package superleague.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Comparator;
import java.time.LocalDate;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import superleague.data.SquadLoader;
import superleague.game.Ending;
import superleague.game.OwnerReview;
import superleague.game.Perk;
import superleague.game.Reward;
import superleague.game.RoundReport;
import superleague.game.Season;
import superleague.game.Verdict;
import superleague.model.Player;
import superleague.model.Tactic;
import superleague.model.Team;
import superleague.model.TrainingIntensity;
import superleague.sim.Fixture;
import superleague.sim.MatchResult;
import superleague.sim.MatchStats;

class GamePanel extends JPanel {
    private final Season season;
    private final Team team;
    private final Runnable onNewGame;

    private final JLabel teamName = Theme.label("", Font.BOLD, 24, Color.WHITE);
    private final JLabel roundInfo = Theme.label("", Font.PLAIN, 14, Theme.ON_DARK_MUTED);
    private final JLabel nextTitle = Theme.label("다음 경기", Font.PLAIN, 12, Theme.ON_DARK_MUTED);
    private final JLabel nextOpponent = Theme.label("", Font.BOLD, 18, Color.WHITE);
    private final JLabel nextDetail = Theme.label("", Font.PLAIN, 13, Theme.ON_DARK_MUTED);

    private final JProgressBar moraleBar = bar(-100, 100);
    private final JProgressBar trainingBar = bar(0, 100);
    private final JProgressBar facilityBar = bar(0, Team.MAX_FACILITY);
    private final JLabel moraleValue = valueLabel();
    private final JLabel trainingValue = valueLabel();
    private final JLabel facilityValue = valueLabel();
    private final JLabel squadInfo = Theme.label("", Font.PLAIN, 12, Theme.MUTED);
    private final JLabel performanceValue = Theme.label("", Font.BOLD, 30, Theme.TEXT);
    private final JLabel performanceDetail = Theme.label("", Font.PLAIN, 12, Theme.MUTED);
    private final JLabel tacticHint = Theme.label("", Font.BOLD, 12, Theme.ACCENT);
    private final JLabel tacticDetail = Theme.label("", Font.PLAIN, 12, Theme.MUTED);
    private final JLabel intensityDetail = Theme.label("", Font.PLAIN, 12, Theme.MUTED);

    private Widgets.Segmented intensitySeg;
    private Widgets.Segmented tacticSeg;
    private final Widgets.Chip tierChip = new Widgets.Chip("", Theme.ACCENT);
    private final Widgets.SeasonProgress seasonProgress = new Widgets.SeasonProgress();
    private final JButton playButton = Theme.primaryButton("경기 진행  ▶");
    private final JButton autoButton = Theme.secondaryButton("시즌 끝까지 자동 진행");

    private final JTabbedPane tabs = new JTabbedPane();
    private final JPanel resultPanel = new JPanel(new BorderLayout(0, 12));
    private final DefaultTableModel standingsModel =
            Tables.model("순위", "구단", "등급", "경기", "승", "무", "패", "득점", "실점", "득실", "승점");
    private final DefaultTableModel squadModel =
            Tables.model("포지션", "이름", "나이", "국적", "키", "몸무게", "주발", "현재", "최대", "성장", "컨디션", "출전", "득점", "상태");
    private final DefaultTableModel historyModel =
            Tables.model("라운드", "장소", "상대", "등급", "스코어", "결과");
    private List<Player> squadRows = List.of();
    private final DefaultTableModel scheduleModel =
            Tables.model("라운드", "날짜", "장소", "상대", "등급", "상대 순위", "결과", "스코어", "득점자");
    private final JTextArea newsArea = new JTextArea();
    private List<Team> standingsSnapshot = List.of();
    private TacticsPanel tactics;
    private TrainingPanel training;

    GamePanel(Season season, SquadLoader.Result data, Runnable onNewGame) {
        super(new BorderLayout());
        this.season = season;
        this.team = season.userTeam();
        this.onNewGame = onNewGame;
        setBackground(Theme.BG);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        add(buildTabs(), BorderLayout.CENTER);

        log(String.format("%s%s 감독으로 부임했습니다. 운영진 목표는 %d위 이내입니다.",
                team.managerName() == null ? "" : team.managerName() + "(" + team.managerStyle().label + ") 님이 ",
                team.name(), team.tier().targetRank()));
        if (team.managerStyle() != null) {
            log("[코칭 스타일] " + team.managerStyle().label + ": " + team.managerStyle().effect);
        }
        log(String.format("총 %d시즌을 치릅니다. 3경기마다 구단주 평가, 17라운드 후 중간 평가, 34라운드 후 시즌 종료 평가가 있습니다.",
                Season.MAX_SEASONS));
        log("[선수 데이터] " + data.summary());
        data.warnings().forEach(w -> log("[선수 데이터] " + w));
        log("[선수 데이터] 선수단 탭에서 * 표시는 자동 생성된 선수입니다.");
        showPlaceholderResult();
        refresh();
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        JPanel nameRow = new JPanel();
        nameRow.setLayout(new BoxLayout(nameRow, BoxLayout.X_AXIS));
        nameRow.setOpaque(false);
        nameRow.setAlignmentX(LEFT_ALIGNMENT);
        nameRow.add(teamName);
        nameRow.add(Box.createHorizontalStrut(10));
        nameRow.add(tierChip);
        if (team.managerStyle() != null) {
            nameRow.add(Box.createHorizontalStrut(8));
            nameRow.add(new Widgets.Chip(team.managerName() + " 감독 · " + team.managerStyle().label, Logo.GOLD.darker()));
        }

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        roundInfo.setAlignmentX(LEFT_ALIGNMENT);
        left.add(nameRow);
        left.add(Box.createVerticalStrut(6));
        left.add(roundInfo);
        JPanel leftWrap = new JPanel(new java.awt.GridBagLayout());
        leftWrap.setOpaque(false);
        leftWrap.add(left);
        header.add(leftWrap, BorderLayout.WEST);

        Widgets.RoundPanel right = new Widgets.RoundPanel(null, 12);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBackground(Theme.NAVY_LIGHT);
        right.setLine(null, 0);
        right.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        right.add(nextTitle);
        right.add(Box.createVerticalStrut(2));
        right.add(nextOpponent);
        right.add(Box.createVerticalStrut(2));
        right.add(nextDetail);
        header.add(right, BorderLayout.EAST);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(header, BorderLayout.CENTER);
        wrap.add(seasonProgress, BorderLayout.SOUTH);
        return wrap;
    }

    private JComponent buildSidebar() {
        JPanel side = new ScrollableColumn();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(Theme.BG);
        side.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 8));

        side.add(stretch(Theme.card("팀 상태", buildStatus())));
        side.add(Box.createVerticalStrut(12));
        side.add(stretch(Theme.card("다음 상대", buildOpponent())));
        side.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(side);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(Theme.BG);

        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
        actions.setBackground(Theme.BG);
        actions.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 8));
        playButton.addActionListener(e -> {
            if (season.isMatchDay()) {
                playRound();
            } else {
                advanceDays(false);
            }
        });
        autoButton.addActionListener(e -> autoToEnd());
        autoButton.setText("끝까지 자동");
        actions.add(playButton);
        actions.add(autoButton);

        JPanel column = new JPanel(new BorderLayout());
        column.setBackground(Theme.BG);
        column.setPreferredSize(new Dimension(372, 0));
        column.add(scroll, BorderLayout.CENTER);
        column.add(actions, BorderLayout.SOUTH);
        return column;
    }

    private static class ScrollableColumn extends JPanel implements javax.swing.Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle r, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle r, int orientation, int direction) {
            return 64;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static JComponent stretch(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    private JComponent buildStatus() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 0, 3, 8);
        g.anchor = GridBagConstraints.WEST;

        addStatRow(p, g, 0, "사기", moraleBar, moraleValue);
        addStatRow(p, g, 1, "훈련 수준", trainingBar, trainingValue);
        addStatRow(p, g, 2, "구단 시설", facilityBar, facilityValue);

        g.gridx = 0;
        g.gridy = 3;
        g.gridwidth = 3;
        g.insets = new Insets(6, 0, 8, 0);
        p.add(squadInfo, g);

        g.gridy = 4;
        g.insets = new Insets(4, 0, 0, 0);
        p.add(Theme.label("예상 경기력", Font.PLAIN, 12, Theme.MUTED), g);
        g.gridy = 5;
        g.insets = new Insets(0, 0, 0, 0);
        p.add(performanceValue, g);
        g.gridy = 6;
        p.add(performanceDetail, g);
        return p;
    }

    private void addStatRow(JPanel p, GridBagConstraints g, int row, String name, JProgressBar bar, JLabel value) {
        g.gridy = row;
        g.gridwidth = 1;
        g.gridx = 0;
        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        JLabel label = Theme.label(name, Font.PLAIN, 13, Theme.TEXT);
        label.setPreferredSize(new Dimension(62, 20));
        p.add(label, g);
        g.gridx = 1;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        p.add(bar, g);
        g.gridx = 2;
        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        p.add(value, g);
    }

    private final JLabel oppName = Theme.label(" ", Font.BOLD, 16, Theme.TEXT);
    private final JLabel oppInfo = Theme.label(" ", Font.PLAIN, 12, Theme.MUTED);
    private final JLabel oppTactic = Theme.label(" ", Font.PLAIN, 12, Theme.TEXT);
    private final JLabel oppScout = Theme.label(" ", Font.BOLD, 12, Theme.WARN);
    private final JPanel oppXi = new JPanel(new java.awt.GridLayout(0, 1, 0, 1));

    private JComponent buildOpponent() {
        oppXi.setOpaque(false);
        JLabel xiTitle = Theme.label("예상 선발", Font.BOLD, 12, Theme.MUTED);
        return stack(oppName, oppInfo, oppTactic, oppScout, xiTitle, oppXi);
    }

    private void refreshOpponent(Fixture next) {
        oppXi.removeAll();
        if (next == null) {
            oppName.setText("시즌 종료");
            oppInfo.setText(" ");
            oppTactic.setText(" ");
            oppScout.setText(" ");
            oppXi.revalidate();
            return;
        }
        Team opp = next.opponentOf(team);
        opp.autoPick();
        oppName.setText(opp.name());
        oppInfo.setText(String.format("%s등급 · %s · %s · %s", opp.tier().name(),
                season.roundsPlayed() == 0 ? "개막 전" : season.rankOf(opp) + "위 승점 " + opp.points(),
                next.home() == team ? "우리 홈" : "상대 홈",
                String.format("%d승 %d무 %d패", opp.won(), opp.drawn(), opp.lost())));
        Tactic style = opp.preferredTactic();
        oppTactic.setText(String.format("<html>운영 <b>%s</b> · 포메이션 <b>%s</b> · 팀 지시 <b>%s</b></html>",
                style.label(), opp.formation(), superleague.model.Instructions.Style.of(opp.instructions()).label));
        boolean ready = team.tactic().counters(style);
        oppScout.setText(ready ? "지금 우리 운영이 상대에 유리합니다."
                : "스카우트: " + style.family().beatenBy().label() + " 운영이 유리합니다.");
        oppScout.setForeground(ready ? Theme.GOOD : Theme.WARN);
        for (int i : TacticsPanel.displayOrder(opp)) {
            superleague.model.Formation.Slot slot = opp.slots().get(i);
            Player p = opp.slotPlayer(i);
            JPanel row = new JPanel(new BorderLayout(6, 0));
            row.setOpaque(false);
            JLabel pos = Theme.label(slot.name(), Font.BOLD, 12, Theme.positionColor(slot.group()));
            pos.setPreferredSize(new java.awt.Dimension(38, 18));
            row.add(pos, BorderLayout.WEST);
            row.add(Theme.label(p == null ? "-" : p.name(), Font.PLAIN, 12, Theme.TEXT), BorderLayout.CENTER);
            row.add(Theme.label(p == null ? "" : String.valueOf(p.rating()), Font.BOLD, 12, Theme.MUTED), BorderLayout.EAST);
            if (p != null) {
                row.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
                row.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        PlayerCard.show(GamePanel.this, p, opp.name());
                    }
                });
            }
            oppXi.add(row);
        }
        oppXi.revalidate();
        oppXi.repaint();
    }

    private JComponent buildIntensity() {
        intensitySeg = new Widgets.Segmented(new String[]{"가볍게", "보통", "강하게"}, i -> {
            team.setIntensity(TrainingIntensity.values()[i]);
            refresh();
        });
        return stack(intensitySeg, intensityDetail);
    }

    private JComponent buildTactic() {
        Tactic[] values = Tactic.values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].label();
        }
        tacticSeg = new Widgets.Segmented(names, i -> {
            team.setTactic(values[i]);
            refresh();
        });
        return stack(tacticSeg, tacticDetail, tacticHint);
    }

    private static JComponent stack(JComponent... parts) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        for (int i = 0; i < parts.length; i++) {
            parts[i].setAlignmentX(LEFT_ALIGNMENT);
            if (i > 0) {
                p.add(Box.createVerticalStrut(i == 1 ? 8 : 4));
            }
            p.add(parts[i]);
        }
        return p;
    }

    private JComponent buildTabs() {
        tabs.setFont(Theme.font(Font.BOLD, 13));
        tabs.setBackground(Theme.BG);

        resultPanel.setBackground(Theme.CARD);
        resultPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        tabs.addTab("라운드 결과", resultPanel);
        tactics = new TacticsPanel(team, this::refresh, buildTactic());
        tabs.addTab("전술", tactics);

        JTable standings = Tables.table(standingsModel,
                row -> row < standingsSnapshot.size() && standingsSnapshot.get(row) == team, 1);
        Tables.widths(standings, 44, 170, 44, 44, 36, 36, 36, 44, 44, 44, 48);
        tabs.addTab("순위표", Tables.scroll(standings));

        JTable schedule = Tables.table(scheduleModel, row -> row == season.roundsPlayed(), 3);
        Tables.widths(schedule, 48, 120, 48, 150, 44, 64, 60, 56, 240);
        tabs.addTab("경기 일정", Tables.scroll(schedule));

        JTable squad = Tables.table(squadModel, row -> false, 1);
        squad.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        javax.swing.table.TableCellRenderer squadBase = squad.getDefaultRenderer(Object.class);
        squad.getColumnModel().getColumn(0).setCellRenderer((t, value, selected, focus, row, column) -> {
            java.awt.Component c = squadBase.getTableCellRendererComponent(t, value, selected, focus, row, column);
            c.setForeground(Theme.positionColor(String.valueOf(value)));
            c.setFont(c.getFont().deriveFont(Font.BOLD));
            return c;
        });
        squad.setToolTipText("선수를 누르면 세부 능력을 볼 수 있습니다.");
        squad.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = squad.rowAtPoint(e.getPoint());
                if (row >= 0 && row < squadRows.size()) {
                    PlayerCard.show(GamePanel.this, squadRows.get(row), team.name());
                }
            }
        });
        Tables.widths(squad, 84, 150, 40, 80, 40, 50, 50, 40, 40, 40, 50, 40, 40, 70);
        tabs.addTab("선수단", Tables.scroll(squad));
        training = new TrainingPanel(team, season, this::refresh, this::advanceDays, buildIntensity());
        tabs.addTab("훈련", training);

        JTable history = Tables.table(historyModel, row -> false, 2);
        Tables.widths(history, 56, 56, 170, 44, 64, 48);
        tabs.addTab("시즌 기록", Tables.scroll(history));

        newsArea.setEditable(false);
        newsArea.setLineWrap(true);
        newsArea.setWrapStyleWord(true);
        newsArea.setFont(Theme.font(Font.PLAIN, 13));
        newsArea.setForeground(Theme.TEXT);
        newsArea.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JScrollPane newsScroll = new JScrollPane(newsArea);
        newsScroll.setBorder(BorderFactory.createEmptyBorder());
        tabs.addTab("소식", newsScroll);

        Widgets.RoundPanel card = new Widgets.RoundPanel(new BorderLayout(), 14);
        card.setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8));
        card.add(tabs, BorderLayout.CENTER);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Theme.BG);
        wrap.setBorder(BorderFactory.createEmptyBorder(16, 8, 16, 16));
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    private void refreshSchedule() {
        scheduleModel.setRowCount(0);
        List<Fixture> fixtures = season.userSchedule();
        List<MatchResult> results = season.userResults();
        for (int i = 0; i < fixtures.size(); i++) {
            Fixture f = fixtures.get(i);
            Team opp = f.opponentOf(team);
            String outcome = i == season.roundsPlayed() ? "다음 경기" : "예정";
            String score = "";
            String scorers = "";
            if (i < results.size()) {
                MatchResult r = results.get(i);
                int gf = r.goalsFor(team);
                int ga = r.goalsAgainst(team);
                outcome = gf > ga ? "승" : gf == ga ? "무" : "패";
                score = gf + " - " + ga;
                scorers = String.join(", ", r.fixture().home() == team ? r.homeScorers() : r.awayScorers());
            }
            scheduleModel.addRow(new Object[]{i + 1, Season.formatDate(season.dateOfRound(i)), f.home() == team ? "홈" : "원정", opp.name(), opp.tier().name(),
                    season.roundsPlayed() == 0 ? "-" : season.rankOf(opp) + "위", outcome, score, scorers});
        }
    }

    private void refresh() {
        int played = season.roundsPlayed();
        refreshSchedule();
        teamName.setText(team.name());
        tierChip.set(team.tier().name() + "등급", Theme.tierColor(team.tier()));
        seasonProgress.setFraction(played / (double) season.totalRounds());
        String rank = played == 0 ? "개막 전" : season.rankOf(team) + "위";
        roundInfo.setText(String.format("시즌 %d/%d (%s) · %s · %d라운드 / %d 진행 · %s · 승점 %d · %d승 %d무 %d패 · 목표 %d위 이내",
                season.seasonNumber(), Season.MAX_SEASONS, season.seasonLabel(), Season.formatDate(season.today()), played, season.totalRounds(), rank, team.points(), team.won(), team.drawn(), team.lost(),
                team.tier().targetRank()));

        Fixture next = season.nextUserFixture();
        refreshOpponent(next);
        if (next != null) {
            Team opp = next.opponentOf(team);
            long dDay = java.time.temporal.ChronoUnit.DAYS.between(season.today(), season.dateOfRound(played));
            nextTitle.setText(String.format("%d라운드 · %s · %s", played + 1, next.home() == team ? "홈" : "원정",
                    dDay == 0 ? "오늘 경기" : "D-" + dDay));
            nextOpponent.setText("vs " + opp.name());
            Tactic oppStyle = opp.preferredTactic();
            nextDetail.setText(String.format("%s등급 · 주 운영 '%s'", opp.tier().name(), oppStyle.label()));
            boolean ready = team.tactic().counters(oppStyle);
            tacticHint.setText(ready
                    ? String.format("상대(%s)에 맞는 대응입니다.", oppStyle.family().label())
                    : String.format("스카우트: 상대가 %s이라 %s 운영이 유리합니다.", oppStyle.family().label(),
                    oppStyle.family().beatenBy().label()));
            tacticHint.setForeground(ready ? Theme.GOOD : Theme.WARN);
        } else {
            nextTitle.setText("시즌 종료");
            nextOpponent.setText("최종 " + season.rankOf(team) + "위");
            nextDetail.setText(String.format("승점 %d · 득실 %+d", team.points(), team.goalDifference()));
            tacticHint.setText(" ");
        }

        moraleBar.setValue(team.morale());
        moraleBar.setForeground(team.morale() >= 0 ? Theme.GOOD : Theme.BAD);
        moraleValue.setText(String.format("%+d  %s", team.morale(), Theme.signed(team.moraleModifier())));
        trainingBar.setValue(team.training());
        trainingValue.setText(String.format("%d/%d  %s", team.training(), team.trainingCap(),
                Theme.signed(team.trainingModifier())));
        facilityBar.setValue(team.facility());
        facilityValue.setText(String.format("Lv.%d  %s", team.facility(), Theme.signed(team.facilityModifier())));
        String perks = (team.teamworkRounds() > 0 ? " · 팀워크 특전 " + team.teamworkRounds() + "경기" : "")
                + (team.trainingBoostRounds() > 0 ? " · 훈련 특전 " + team.trainingBoostRounds() + "경기" : "");
        squadInfo.setText(String.format("평균 컨디션 %.0f · 부상자 %d명%s", team.averageCondition(), team.injuredCount(), perks));
        performanceValue.setText(String.format("%.1f", team.performance()));
        performanceDetail.setText(String.format("선발 %.1f × (1 %s %s %s %s)", team.lineupStrength(),
                Theme.signed(team.moraleModifier()), Theme.signed(team.trainingModifier()),
                Theme.signed(team.facilityModifier()), Theme.signed(team.chemistryModifier())));

        TrainingIntensity ti = team.intensity();
        intensitySeg.setSelectedIndex(ti.ordinal());
        intensityDetail.setText(String.format("훈련 %+d · 사기 %+d · 컨디션 %+d · 부상 위험 %.1f%%",
                ti.trainingDelta(), ti.moraleDelta(), ti.conditionDelta(), ti.injuryChance() * 100));
        Tactic tc = team.tactic();
        tacticSeg.setSelectedIndex(tc.ordinal());
        tacticDetail.setText(String.format("득점 x%.2f · 실점 x%.2f · %s 상대로 유리",
                tc.attackFactor(), tc.concedeFactor(), tc.beatsLabel()));

        refreshStandings();
        refreshSquad();

        boolean running = !season.isFinished();
        tactics.refresh(running);
        training.refresh(running);
        playButton.setEnabled(running);
        playButton.setText(season.isMatchDay() ? "경기 진행  ▶" : "다음 날  ▶");
        autoButton.setEnabled(running);
        intensitySeg.setEnabled(running);
        tacticSeg.setEnabled(running);
    }

    private void refreshStandings() {
        standingsSnapshot = season.standings();
        standingsModel.setRowCount(0);
        for (int i = 0; i < standingsSnapshot.size(); i++) {
            Team t = standingsSnapshot.get(i);
            standingsModel.addRow(new Object[]{i + 1, t.name(), t.tier().name(), t.played(), t.won(), t.drawn(),
                    t.lost(), t.goalsFor(), t.goalsAgainst(), String.format("%+d", t.goalDifference()), t.points()});
        }
    }

    private void refreshSquad() {
        List<Player> xi = team.startingEleven();
        List<Player> squadSnapshot = squadRows = team.squad().stream()
                .sorted(Comparator.comparing(Player::position)
                        .thenComparing(Comparator.comparingInt(Player::rating).reversed()))
                .toList();
        squadModel.setRowCount(0);
        for (Player p : squadSnapshot) {
            String status = !p.isAvailable() ? "부상 (" + p.injuredRounds() + ")" : xi.contains(p) ? "선발" : "";
            String name = p.isGenerated() ? p.name() + " *" : p.name();
            String growth = p.growth() > 0 ? "+" + p.growth() : "";
            squadModel.addRow(new Object[]{p.positions(), name, p.age(), p.nationality(), p.height(), p.weight(),
                    p.foot().label(), p.rating(), p.potential(), growth, p.condition(), p.appearances(), p.goals(), status});
        }
    }

    void advanceDays(boolean toMatchDay) {
        do {
            LocalDate day = season.today();
            String activity = season.activityOn(day).label;
            List<String> news = season.advanceDay();
            log(String.format("[%s] %s", Season.formatDate(day), activity));
            news.forEach(this::log);
        } while (toMatchDay && !season.isMatchDay() && !season.isFinished());
        refresh();
    }

    private void playRound() {
        int round = season.roundsPlayed() + 1;
        RoundReport report = season.playNextRound(live -> LiveMatchDialog.show(this, live, team, round));
        recordRound(report);
        showRoundResult(report);
        tabs.setSelectedIndex(0);
        refresh();
        afterRound(report);
    }

    private void autoToEnd() {
        int answer = Dialogs.show(this, "자동 진행", "시즌 끝까지 자동으로 진행할까요?",
                List.of("현재 훈련 강도, 경기 운영, 전술을 그대로 유지한 채 남은 경기를 모두 치릅니다.",
                        "중간 평가와 시즌 종료 평가는 진행 중에 따로 보여 줍니다."),
                Dialogs.Tone.INFO, null, "취소", "자동 진행");
        if (answer != 1) {
            return;
        }
        RoundReport last = null;
        while (!season.isFinished()) {
            last = season.playNextRound();
            recordRound(last);
            if (last.ownerReview() != null) {
                ownerEvent(last.ownerReview(), true);
            }
            if (last.round() == Season.MID_SEASON_ROUND) {
                showRoundResult(last);
                refresh();
                review("중간 평가");
            }
        }
        if (last != null) {
            showRoundResult(last);
        }
        refresh();
        finishSeason();
    }

    private void afterRound(RoundReport report) {
        if (report.ownerReview() != null) {
            ownerEvent(report.ownerReview(), false);
            refresh();
        }
        if (report.round() == Season.MID_SEASON_ROUND) {
            review("중간 평가");
            refresh();
        }
        if (season.isFinished()) {
            finishSeason();
        }
    }

    private void recordRound(RoundReport report) {
        MatchResult r = report.userResult();
        int gf = r.goalsFor(team);
        int ga = r.goalsAgainst(team);
        String outcome = gf > ga ? "승" : gf == ga ? "무" : "패";
        Team opp = r.fixture().opponentOf(team);
        historyModel.insertRow(0, new Object[]{report.round(), r.fixture().home() == team ? "홈" : "원정",
                opp.name(), opp.tier().name(), gf + " - " + ga, outcome});
        log(String.format("[R%d] %s  %s", report.round(), outcome, r.scoreLine()));
        report.reactions().forEach(n -> log(String.format("[R%d] %s", report.round(), n)));
        report.news().forEach(n -> log(String.format("[R%d] %s", report.round(), n)));
    }

    private void ownerEvent(OwnerReview review, boolean auto) {
        String title = "구단주 평가";
        log("[" + title + "] " + review.headline());
        List<String> lines = new java.util.ArrayList<>(review.lines());
        if (review.grade().equals("대만족")) {
            lines.add("기대 이상의 결과에 구단주가 선수단 전체에 격려금을 보냈습니다. (사기 +10)");
        }
        if (review.penalty() != null) {
            lines.add(review.penalty());
            log("[" + title + "] " + review.penalty());
        }
        if (!review.reward()) {
            if (review.penalty() == null) {
                lines.add("구단주는 기대한 만큼의 결과라며 별다른 말을 하지 않았습니다.");
            }
            if (!auto) {
                Dialogs.show(this, title, review.headline(), lines,
                        review.penalty() != null ? Dialogs.Tone.WARN : Dialogs.Tone.INFO, null, "확인");
            }
            return;
        }
        Perk[] perks = Perk.values();
        int choice = 0;
        if (!auto) {
            List<Dialogs.Option> options = new java.util.ArrayList<>();
            for (Perk p : perks) {
                String detail = p == Perk.PLAYER ? p.effect + " · 대상: " + review.mvp().name() : p.effect;
                options.add(new Dialogs.Option(p.label, detail));
            }
            lines.add("성과에 만족한 구단주가 특전을 하나 제안합니다.");
            choice = Math.max(0, Math.min(perks.length - 1,
                    Dialogs.show(this, title + " · 특전 선택", review.headline(), lines, Dialogs.Tone.GOOD, options)));
        }
        String msg = season.claimPerk(perks[choice]);
        if (msg != null) {
            log("[" + title + "] " + msg);
        }
    }

    private void showPlaceholderResult() {
        resultPanel.removeAll();
        JLabel hint = Theme.label("<html><div style='text-align:center'>훈련 강도와 전술을 정한 뒤<br>"
                + "왼쪽의 <b>경기 진행</b>을 누르세요.</div></html>", Font.PLAIN, 15, Theme.MUTED);
        hint.setHorizontalAlignment(JLabel.CENTER);
        resultPanel.add(hint, BorderLayout.CENTER);
        resultPanel.revalidate();
        resultPanel.repaint();
    }

    private void showRoundResult(RoundReport report) {
        resultPanel.removeAll();
        MatchResult r = report.userResult();
        int gf = r.goalsFor(team);
        int ga = r.goalsAgainst(team);
        Color tone = gf > ga ? Theme.GOOD : gf == ga ? Theme.MUTED : Theme.BAD;
        String outcome = gf > ga ? "승리" : gf == ga ? "무승부" : "패배";

        JPanel board = new JPanel(new BorderLayout(0, 6));
        board.setBackground(Theme.BG);
        board.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, tone),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        JLabel caption = Theme.label(String.format("%d라운드 · %s", report.round(), outcome), Font.BOLD, 13, tone);
        board.add(caption, BorderLayout.NORTH);

        JPanel scoreRow = new JPanel(new GridLayout(1, 3));
        scoreRow.setOpaque(false);
        JLabel home = Theme.label(r.fixture().home().name(), Font.BOLD, 18, Theme.TEXT);
        home.setHorizontalAlignment(JLabel.RIGHT);
        JLabel score = Theme.label(r.homeGoals() + " : " + r.awayGoals(), Font.BOLD, 34, Theme.TEXT);
        score.setHorizontalAlignment(JLabel.CENTER);
        JLabel away = Theme.label(r.fixture().away().name(), Font.BOLD, 18, Theme.TEXT);
        scoreRow.add(home);
        scoreRow.add(score);
        scoreRow.add(away);
        board.add(scoreRow, BorderLayout.CENTER);

        JPanel scorers = new JPanel(new GridLayout(1, 3));
        scorers.setOpaque(false);
        JLabel hs = Theme.label(scorerHtml(r.homeScorers(), "right"), Font.PLAIN, 12, Theme.MUTED);
        hs.setHorizontalAlignment(JLabel.RIGHT);
        hs.setVerticalAlignment(JLabel.TOP);
        JLabel as = Theme.label(scorerHtml(r.awayScorers(), "left"), Font.PLAIN, 12, Theme.MUTED);
        as.setVerticalAlignment(JLabel.TOP);
        scorers.add(hs);
        scorers.add(new JLabel());
        scorers.add(as);
        GoalTimeline stones = new GoalTimeline();
        stones.setEvents(r.events());
        JPanel replayRow = new JPanel(new BorderLayout());
        replayRow.setOpaque(false);
        MatchStats st = r.stats();
        replayRow.add(Theme.label(String.format("슈팅 %d(%d) : %d(%d) · 점유율 %d%% : %d%%",
                st.homeShots(), st.homeOnTarget(), st.awayShots(), st.awayOnTarget(),
                st.homePossession(), st.awayPossession()), Font.PLAIN, 12, Theme.MUTED), BorderLayout.WEST);
        JButton replay = Theme.secondaryButton("문자 중계 다시 보기");
        replay.addActionListener(e -> LiveMatchDialog.show(this, r, team, report.round()));
        replayRow.add(replay, BorderLayout.EAST);
        JPanel south = new JPanel(new BorderLayout(0, 10));
        south.setOpaque(false);
        south.add(scorers, BorderLayout.NORTH);
        south.add(stones, BorderLayout.CENTER);
        south.add(replayRow, BorderLayout.SOUTH);
        board.add(south, BorderLayout.SOUTH);
        resultPanel.add(board, BorderLayout.NORTH);

        DefaultTableModel others = Tables.model("홈", "스코어", "원정");
        for (MatchResult m : report.results()) {
            if (m != r) {
                others.addRow(new Object[]{m.fixture().home().name(), m.homeGoals() + " - " + m.awayGoals(),
                        m.fixture().away().name()});
            }
        }
        JTable table = Tables.table(others, row -> false);
        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setOpaque(false);
        center.add(Theme.label("같은 라운드 다른 경기", Font.BOLD, 13, Theme.TEXT), BorderLayout.NORTH);
        center.add(Tables.scroll(table), BorderLayout.CENTER);
        resultPanel.add(center, BorderLayout.CENTER);

        if (!report.news().isEmpty() || !report.reactions().isEmpty()) {
            StringBuilder sb = new StringBuilder("<html><b>경기 후 반응</b>");
            report.reactions().forEach(n -> sb.append("<br>· ").append(n));
            if (!report.news().isEmpty()) {
                sb.append("<br><br><b>구단 소식</b>");
            }
            report.news().forEach(n -> sb.append("<br>· ").append(n));
            sb.append("</html>");
            JLabel news = Theme.label(sb.toString(), Font.PLAIN, 13, Theme.TEXT);
            news.setOpaque(true);
            news.setBackground(new Color(0xFFF7E6));
            news.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            resultPanel.add(news, BorderLayout.SOUTH);
        }
        resultPanel.revalidate();
        resultPanel.repaint();
    }

    private static String scorerHtml(List<String> scorers, String align) {
        if (scorers.isEmpty()) {
            return " ";
        }
        return "<html><div style='text-align:" + align + "'>" + String.join("<br>", scorers) + "</div></html>";
    }

    private void review(String title) {
        int rank = season.rankOf(team);
        Verdict verdict = Verdict.of(team.tier(), rank);
        String head = String.format("현재 %d위 · 목표 %d위 이내 → %s", rank, team.tier().targetRank(), verdict.label());
        List<String> messages = season.boardReview(t -> askReward(title, head));
        List<String> lines = new java.util.ArrayList<>();
        switch (verdict) {
            case OUTSTANDING -> lines.add("기대를 크게 넘어선 성적에 운영진이 시설 투자와 보너스를 모두 결정했습니다.");
            case NEAR -> lines.add("운영진은 기대 수준이라며 별도 조치를 하지 않았습니다.");
            default -> {
            }
        }
        messages.forEach(m -> lines.add("· " + m));
        log("[" + title + "] " + head);
        messages.forEach(m -> log("[" + title + "] " + m));
        Dialogs.Tone tone = switch (verdict) {
            case OUTSTANDING, MET -> Dialogs.Tone.GOOD;
            case MISSED -> Dialogs.Tone.WARN;
            default -> Dialogs.Tone.INFO;
        };
        Dialogs.show(this, title, head, lines, tone, null, "확인");
    }

    private Reward askReward(String title, String head) {
        Reward[] rewards = Reward.values();
        List<Dialogs.Option> options = new java.util.ArrayList<>();
        for (Reward r : rewards) {
            options.add(new Dialogs.Option(r.label(), r.effect()));
        }
        int choice = Dialogs.show(this, title + " · 운영진 보상", head,
                List.of("목표를 달성해 운영진이 보상을 제안합니다. 하나를 고르세요."),
                Dialogs.Tone.GOOD, options);
        return rewards[Math.max(0, Math.min(rewards.length - 1, choice))];
    }

    private void finishSeason() {
        review("시즌 종료 평가");
        refresh();
        int rank = season.rankOf(team);
        Ending ending = Ending.decide(team, rank);
        Player topScorer = team.squad().stream().max(Comparator.comparingInt(Player::goals)).orElseThrow();
        season.closeSeason(topScorer);
        log("[" + season.seasonLabel() + " 시즌 결과] " + ending.title());
        if (ending.fired() || season.isLastSeason()) {
            Ending last = ending.fired() ? ending
                    : Ending.career(team, season.titles(), season.targetsMet(), season.seasonNumber());
            log("[엔딩] " + last.title());
            if (EndingDialog.show(this, team, rank, last, topScorer, season.career())) {
                onNewGame.run();
            }
            return;
        }
        List<String> lines = new java.util.ArrayList<>();
        lines.add(ending.description());
        lines.add(season.career().get(season.career().size() - 1));
        lines.add("다음 시즌: 모든 선수 나이 +1 · 31세 이상은 능력 하락 가능 · 부상/컨디션 초기화 · 사기는 절반으로");
        Dialogs.show(this, season.seasonLabel() + " 시즌 종료", String.format("시즌 %d/%d · %s",
                season.seasonNumber(), Season.MAX_SEASONS, ending.title()), lines, Dialogs.Tone.GOOD, null, "다음 시즌 시작");
        List<String> news = season.startNextSeason();
        log(String.format("──── 시즌 %d/%d (%s) 개막 ────", season.seasonNumber(), Season.MAX_SEASONS, season.seasonLabel()));
        news.forEach(n -> log("[새 시즌] " + n));
        historyModel.setRowCount(0);
        showPlaceholderResult();
        refresh();
    }

    private void log(String line) {
        newsArea.append(line + "\n");
        newsArea.setCaretPosition(newsArea.getDocument().getLength());
    }

    private static JProgressBar bar(int min, int max) {
        JProgressBar bar = new JProgressBar(min, max);
        bar.setUI(new FlatUI.Progress());
        bar.setBorderPainted(false);
        bar.setBackground(Theme.TRACK);
        bar.setForeground(Theme.ACCENT);
        bar.setPreferredSize(new Dimension(120, 12));
        return bar;
    }

    private static JLabel valueLabel() {
        JLabel label = Theme.label("", Font.PLAIN, 12, Theme.MUTED);
        label.setPreferredSize(new Dimension(100, 20));
        return label;
    }
}
