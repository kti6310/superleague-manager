package superleague.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
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
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import superleague.model.Player;
import superleague.model.Team;
import superleague.sim.MatchEvent;
import superleague.sim.LiveMatch;
import superleague.sim.MatchResult;
import superleague.sim.MatchStats;

final class LiveMatchDialog extends JDialog {
    private static final int[] SPEED_MS = {230, 120, 55};

    private final LiveMatch live;
    private MatchResult result;
    private final Team userTeam;
    private final Kits.Match kits;
    private List<MatchEvent> events;
    private int lastKey = -1;
    private int[] lastStep = {0, 0};
    private boolean paused;
    private final List<int[]> steps = new ArrayList<>();
    private int stepIndex;
    private int eventIndex;
    private int pauseTicks;
    private int speed = 0;
    private boolean finished;

    private final JLabel homeScore = Theme.label("0", Font.BOLD, 40, Color.WHITE);
    private final JLabel awayScore = Theme.label("0", Font.BOLD, 40, Color.WHITE);
    private final JLabel clock = Theme.label("0'", Font.BOLD, 15, Theme.ACCENT_BRIGHT);
    private final JLabel status = Theme.label("킥오프 대기", Font.PLAIN, 12, Theme.ON_DARK_MUTED);
    private final PitchView pitch;
    private boolean kickoffNext;
    private final JPanel feed = new JPanel();
    private final JScrollPane feedScroll;
    private final StatRow possession = new StatRow("점유율");
    private final StatRow shots = new StatRow("슈팅");
    private final StatRow onTarget = new StatRow("유효 슈팅");
    private final StatRow corners = new StatRow("코너킥");
    private final StatRow cards = new StatRow("경고");
    private final JButton skipButton = Theme.secondaryButton("결과 바로 보기");
    private final JButton closeButton = Theme.primaryButton("닫기");
    private final JButton pauseButton = Theme.secondaryButton("일시정지");
    private final JButton subButton = Theme.primaryButton("선수 교체");
    private final JButton tacticButton = Theme.secondaryButton("전술 변경");
    private final JLabel staminaList = Theme.label(" ", Font.PLAIN, 12, Theme.TEXT);
    private final Timer timer;

    private LiveMatchDialog(Window owner, LiveMatch live, MatchResult result, Team userTeam, String round) {
        super(owner, "경기 중계", ModalityType.APPLICATION_MODAL);
        this.live = live;
        this.result = result;
        this.userTeam = userTeam;
        this.kits = Kits.forMatch(result.fixture().home(), result.fixture().away());
        this.events = result.events();
        buildSteps();

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(header(round), BorderLayout.NORTH);

        pitch = new PitchView(result.fixture().home(), result.fixture().away(), result.stats().homePossession());
        JPanel timelineWrap = new JPanel(new BorderLayout(0, 8));
        timelineWrap.setOpaque(false);
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        legend.setOpaque(false);
        legend.add(Theme.label("경기장", Font.BOLD, 13, Theme.TEXT));
        legend.add(Box.createHorizontalStrut(8));
        legend.add(new StoneIcon(kits.home(), 6));
        legend.add(Theme.label(result.fixture().home().name() + " (홈)", Font.PLAIN, 12, Theme.MUTED));
        legend.add(Box.createHorizontalStrut(6));
        legend.add(new StoneIcon(kits.away(), 6));
        legend.add(Theme.label(result.fixture().away().name() + " (원정)", Font.PLAIN, 12, Theme.MUTED));
        timelineWrap.add(legend, BorderLayout.NORTH);
        timelineWrap.add(pitch, BorderLayout.CENTER);

        feed.setLayout(new BoxLayout(feed, BoxLayout.Y_AXIS));
        feed.setBackground(Theme.CARD);
        feed.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        JPanel feedHolder = new JPanel(new BorderLayout());
        feedHolder.setBackground(Theme.CARD);
        feedHolder.add(feed, BorderLayout.NORTH);
        feedScroll = new JScrollPane(feedHolder, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        feedScroll.setBorder(BorderFactory.createEmptyBorder());
        feedScroll.getVerticalScrollBar().setUnitIncrement(16);
        feedScroll.getViewport().setBackground(Theme.CARD);
        Widgets.RoundPanel feedCard = new Widgets.RoundPanel(new BorderLayout(0, 6), 14);
        feedCard.setBorder(BorderFactory.createEmptyBorder(12, 4, 8, 4));
        JLabel feedTitle = Theme.label("문자 중계", Font.BOLD, 13, Theme.TEXT);
        feedTitle.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        feedCard.add(feedTitle, BorderLayout.NORTH);
        feedCard.add(feedScroll, BorderLayout.CENTER);

        JPanel statsBody = new JPanel(new GridLayout(0, 2, 18, 8));
        statsBody.setOpaque(false);
        for (StatRow r : new StatRow[]{possession, shots, onTarget, corners, cards}) {
            statsBody.add(r);
        }
        JPanel statsCard = Theme.card("경기 기록", statsBody);
        timelineWrap.add(statsCard, BorderLayout.SOUTH);

        JPanel middle = new JPanel(new BorderLayout(14, 0));
        middle.setOpaque(false);
        middle.setBorder(BorderFactory.createEmptyBorder(12, 18, 10, 18));
        middle.add(timelineWrap, BorderLayout.WEST);
        middle.add(feedCard, BorderLayout.CENTER);
        if (live != null) {
            staminaList.setVerticalAlignment(JLabel.TOP);
            JPanel staminaCard = Theme.card("우리 팀 체력", staminaList);
            staminaCard.setPreferredSize(new Dimension(230, 10));
            middle.add(staminaCard, BorderLayout.EAST);
        }
        root.add(middle, BorderLayout.CENTER);
        root.add(footer(), BorderLayout.SOUTH);

        setContentPane(root);
        setSize(live != null ? 1360 : 1120, 790);
        setMinimumSize(new Dimension(1000, 720));
        setLocationRelativeTo(owner);
        updateStats();

        timer = new Timer(SPEED_MS[0], e -> tick());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                timer.start();
                pitch.start();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                finishNow();
            }
        });
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }

    static void show(Component parent, MatchResult result, Team userTeam, String round) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        new LiveMatchDialog(owner, null, result, userTeam, round).setVisible(true);
    }

    static void show(Component parent, LiveMatch live, Team userTeam, String round) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        new LiveMatchDialog(owner, live, live.result(), userTeam, round).setVisible(true);
    }

    private JComponent header(String round) {
        JPanel h = new JPanel(new GridBagLayout());
        h.setBackground(Theme.NAVY);
        h.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.insets = new Insets(0, 12, 0, 12);

        c.gridx = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.EAST;
        h.add(teamBlock(result.fixture().home(), true), c);

        JPanel mid = new JPanel();
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        mid.setOpaque(false);
        JPanel score = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        score.setOpaque(false);
        score.add(homeScore);
        score.add(Theme.label(":", Font.BOLD, 32, Theme.ON_DARK_MUTED));
        score.add(awayScore);
        JLabel roundLabel = Theme.label(round, Font.PLAIN, 12, Theme.ON_DARK_MUTED);
        for (JComponent comp : new JComponent[]{roundLabel, score, clock, status}) {
            comp.setAlignmentX(CENTER_ALIGNMENT);
            mid.add(comp);
        }
        c.gridx = 1;
        c.weightx = 0;
        c.anchor = GridBagConstraints.CENTER;
        h.add(mid, c);

        c.gridx = 2;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        h.add(teamBlock(result.fixture().away(), false), c);
        return h;
    }

    private JComponent teamBlock(Team team, boolean home) {
        JPanel p = new JPanel(new FlowLayout(home ? FlowLayout.RIGHT : FlowLayout.LEFT, 10, 0));
        p.setOpaque(false);
        JLabel name = Theme.label(team.name(), Font.BOLD, 20, Color.WHITE);
        JLabel tag = Theme.label((home ? "홈" : "원정") + (team == userTeam ? " · 내 팀" : ""), Font.PLAIN, 12,
                team == userTeam ? Theme.ACCENT_BRIGHT : Theme.ON_DARK_MUTED);
        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        name.setAlignmentX(home ? RIGHT_ALIGNMENT : LEFT_ALIGNMENT);
        tag.setAlignmentX(home ? RIGHT_ALIGNMENT : LEFT_ALIGNMENT);
        text.add(name);
        text.add(tag);
        StoneIcon stone = new StoneIcon(home ? kits.home() : kits.away(), 11);
        if (home) {
            p.add(text);
            p.add(stone);
        } else {
            p.add(stone);
            p.add(text);
        }
        return p;
    }

    private JComponent footer() {
        JPanel f = new JPanel(new BorderLayout());
        f.setBackground(Theme.CARD);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(Theme.label("중계 속도", Font.BOLD, 13, Theme.TEXT));
        Widgets.Segmented speedSeg = new Widgets.Segmented(new String[]{"1x", "2x", "4x"}, i -> {
            speed = i;
            timer.setDelay(SPEED_MS[i]);
        }) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(210, 38);
            }
        };
        speedSeg.setSelectedIndex(0);
        left.add(speedSeg);
        f.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        skipButton.addActionListener(e -> finishNow());
        pauseButton.addActionListener(e -> setPaused(!paused));
        subButton.addActionListener(e -> openSubstitution());
        tacticButton.addActionListener(e -> openTactics());
        subButton.setEnabled(false);
        updateSubButton();
        closeButton.setEnabled(false);
        closeButton.addActionListener(e -> {
            pitch.stop();
            dispose();
        });
        if (live != null) {
            right.add(tacticButton);
            right.add(subButton);
        }
        right.add(pauseButton);
        right.add(skipButton);
        right.add(closeButton);
        f.add(right, BorderLayout.EAST);
        getRootPane().setDefaultButton(closeButton);
        return f;
    }

    private static int key(MatchEvent e) {
        return e.order();
    }

    private void setPaused(boolean p) {
        paused = p;
        pauseButton.setText(p ? "계속" : "일시정지");
        if (p) {
            timer.stop();
            status.setText("일시정지");
        } else if (!finished) {
            timer.start();
        }
    }

    private void updateSubButton() {
        if (live == null) {
            return;
        }
        int left = live.subsLeft(userTeam);
        int windows = live.windowsLeft(userTeam);
        subButton.setText(String.format("선수 교체 (%d명 · %d번 남음)", left, windows));
        subButton.setEnabled(!finished && stepIndex > 0 && left > 0 && windows > 0);
        tacticButton.setEnabled(!finished && stepIndex > 0);
        updateStamina();
    }

    private int currentClock() {
        return lastStep[0] < 0 ? 45 : lastStep[0];
    }

    private void updateStamina() {
        if (live == null) {
            return;
        }
        StringBuilder sb = new StringBuilder("<html>");
        for (int i = 0; i < 11; i++) {
            Player p = userTeam.slotPlayer(i);
            if (p != null) {
                sb.append("<b>").append(userTeam.slots().get(i).name()).append("</b> ").append(p.name()).append("<br>&nbsp;&nbsp;")
                        .append(SubstitutionDialog.staminaText(live.stamina(userTeam, p, currentClock()))).append("<br>");
            }
        }
        staminaList.setText(sb.append("</html>").toString());
    }

    private void openTactics() {
        boolean wasPaused = paused;
        setPaused(true);
        javax.swing.JComboBox<superleague.model.Tactic> tactic = new javax.swing.JComboBox<>(superleague.model.Tactic.values());
        tactic.setSelectedItem(userTeam.tactic());
        java.util.List<superleague.model.Instructions.Style> styles = new ArrayList<>(java.util.List.of(superleague.model.Instructions.Style.values()));
        styles.remove(superleague.model.Instructions.Style.CUSTOM);
        javax.swing.JComboBox<superleague.model.Instructions.Style> style = new javax.swing.JComboBox<>(
                styles.toArray(new superleague.model.Instructions.Style[0]));
        superleague.model.Instructions.Style current = superleague.model.Instructions.Style.of(userTeam.instructions());
        style.setSelectedItem(current == superleague.model.Instructions.Style.CUSTOM ? superleague.model.Instructions.Style.BALANCED : current);
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 6));
        form.add(Theme.label("경기 운영", Font.BOLD, 13, Theme.TEXT));
        form.add(tactic);
        form.add(Theme.label("팀 스타일 (압박·템포·라인·폭)", Font.BOLD, 13, Theme.TEXT));
        form.add(style);
        form.add(Theme.label("<html>바꾼 순간부터 남은 시간이 새 전술로 다시 계산됩니다.<br>경기가 끝나면 원래 전술로 돌아갑니다.</html>",
                Font.PLAIN, 12, Theme.MUTED));
        int ok = javax.swing.JOptionPane.showConfirmDialog(this, form, "경기 중 전술 변경",
                javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.PLAIN_MESSAGE);
        if (ok == javax.swing.JOptionPane.OK_OPTION) {
            boolean ht = lastStep[0] < 0;
            if (live.changeTactic(userTeam, (superleague.model.Tactic) tactic.getSelectedItem(),
                    (superleague.model.Instructions.Style) style.getSelectedItem(),
                    ht ? 46 : lastStep[0], ht ? 0 : lastStep[1], lastKey)) {
                applyLiveUpdate();
            }
        }
        if (!wasPaused) {
            setPaused(false);
        }
    }

    private void applyLiveUpdate() {
        result = live.result();
        events = result.events();
        reveal(events.get(eventIndex++));
        updateStats();
        updateSubButton();
        pitch.repaint();
    }

    private void openSubstitution() {
        boolean wasPaused = paused;
        setPaused(true);
        SubstitutionDialog.show(this, live, userTeam, currentClock(), changes -> {
            boolean ht = lastStep[0] < 0;
            int minute = ht ? 46 : lastStep[0];
            int extra = ht ? 0 : lastStep[1];
            int before = eventIndex;
            if (live.substitute(userTeam, changes, minute, extra, lastKey)) {
                result = live.result();
                events = result.events();
                eventIndex = before;
                for (int i = 0; i < changes.size(); i++) {
                    reveal(events.get(eventIndex++));
                }
                updateStats();
                pitch.repaint();
            }
        });
        updateSubButton();
        if (!wasPaused) {
            setPaused(false);
        }
    }

    private void buildSteps() {
        int firstExtra = 0;
        int lastExtra = 0;
        for (MatchEvent e : events) {
            if (e.minute() == 45 && e.type() != MatchEvent.Type.HALFTIME) {
                firstExtra = Math.max(firstExtra, e.extra());
            }
            if (e.minute() == 90) {
                lastExtra = Math.max(lastExtra, e.extra());
            }
        }
        for (int m = 0; m <= 45; m++) {
            steps.add(new int[]{m, 0});
        }
        for (int x = 1; x <= firstExtra; x++) {
            steps.add(new int[]{45, x});
        }
        steps.add(new int[]{-1, -1});
        for (int m = 46; m <= 90; m++) {
            steps.add(new int[]{m, 0});
        }
        for (int x = 1; x <= lastExtra; x++) {
            steps.add(new int[]{90, x});
        }
    }

    private void tick() {
        if (finished) {
            return;
        }
        if (pauseTicks > 0) {
            pauseTicks--;
            return;
        }
        if (stepIndex >= steps.size()) {
            finishNow();
            return;
        }
        int[] step = steps.get(stepIndex++);
        int stepKey = step[0] < 0 ? 459 : step[0] * 10 + step[1];
        lastKey = stepKey;
        lastStep = step;
        updateSubButton();
        if (step[0] >= 0) {
            clock.setText(step[1] > 0 ? step[0] + "+" + step[1] + "'" : step[0] + "'");
            status.setText(step[0] <= 45 && stepKey < 459 ? "전반전" : "후반전");
        }
        List<MatchEvent> fresh = new ArrayList<>();
        boolean goal = false;
        while (eventIndex < events.size() && key(events.get(eventIndex)) <= stepKey) {
            MatchEvent e = events.get(eventIndex++);
            reveal(e);
            fresh.add(e);
            if (e.type() == MatchEvent.Type.GOAL) {
                goal = true;
                pauseTicks = speed == 2 ? 10 : 16;
            } else if (e.type() == MatchEvent.Type.HALFTIME) {
                pitch.switchSides();
                kickoffNext = false;
                pauseTicks = speed == 2 ? 14 : 22;
                status.setText("하프타임");
                clock.setText("HT");
            } else if (e.isKey()) {
                pauseTicks = speed == 2 ? 2 : 4;
            } else if (e.type() == MatchEvent.Type.FULLTIME) {
                finishNow();
                return;
            }
        }
        if (fresh.stream().noneMatch(e -> e.type() == MatchEvent.Type.HALFTIME)) {
            if (kickoffNext && fresh.isEmpty()) {
                pitch.kickoff();
            } else {
                pitch.step(fresh, events.subList(eventIndex, Math.min(events.size(), eventIndex + 3)));
            }
            kickoffNext = goal;
        }
        updateStats();
    }

    private void finishNow() {
        if (finished) {
            pitch.stop();
            dispose();
            return;
        }
        while (eventIndex < events.size()) {
            reveal(events.get(eventIndex++));
        }
        finished = true;
        timer.stop();
        pauseButton.setEnabled(false);
        updateSubButton();
        clock.setText("FT");
        status.setText("경기 종료");
        updateStats();
        skipButton.setEnabled(false);
        closeButton.setEnabled(true);
        closeButton.requestFocusInWindow();
        if (result.fixture().involves(userTeam)) {
            // big full-time card: win / draw / loss, final score and scorers
            MatchResultOverlay.show(getRootPane(), result, userTeam, kits, () -> {
                pitch.stop();
                dispose();
            });
        }
    }

    private void reveal(MatchEvent e) {
        homeScore.setText(String.valueOf(e.homeScore()));
        awayScore.setText(String.valueOf(e.awayScore()));
        int width = Math.max(200, feedScroll.getViewport().getWidth() - 190);
        feed.add(new FeedRow(e, width, kits));
        feed.revalidate();
        SwingUtilities.invokeLater(() -> {
            javax.swing.JScrollBar bar = feedScroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    private void updateStats() {
        int[] sh = new int[2];
        int[] ot = new int[2];
        int[] yc = new int[2];
        for (int i = 0; i < eventIndex; i++) {
            MatchEvent e = events.get(i);
            int s = e.side() == MatchEvent.Side.HOME ? 0 : 1;
            switch (e.type()) {
                case GOAL -> {
                    if (!e.ownGoal()) {
                        sh[s]++;
                        ot[s]++;
                    }
                }
                case SAVE, BIG_SAVE -> {
                    sh[s]++;
                    ot[s]++;
                }
                case MISS, BLOCK, WOODWORK -> sh[s]++;
                case YELLOW -> yc[s]++;
                default -> {
                }
            }
        }
        MatchStats st = result.stats();
        double frac = finished ? 1.0 : Math.min(1.0, stepIndex / (double) Math.max(1, steps.size()));
        possession.set(stepIndex == 0 ? 50 : st.homePossession(), stepIndex == 0 ? 50 : st.awayPossession(), "%");
        shots.set(sh[0], sh[1], "");
        onTarget.set(ot[0], ot[1], "");
        corners.set((int) Math.round(st.homeCorners() * frac), (int) Math.round(st.awayCorners() * frac), "");
        cards.set(yc[0], yc[1], "");
    }

    private static class StatRow extends JPanel {
        private final JLabel left = Theme.label("0", Font.BOLD, 14, Theme.TEXT);
        private final JLabel right = Theme.label("0", Font.BOLD, 14, Theme.TEXT);
        private final Bar bar = new Bar();

        StatRow(String name) {
            super(new BorderLayout(0, 4));
            setOpaque(false);
            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            top.add(left, BorderLayout.WEST);
            JLabel n = Theme.label(name, Font.PLAIN, 12, Theme.MUTED);
            n.setHorizontalAlignment(JLabel.CENTER);
            top.add(n, BorderLayout.CENTER);
            top.add(right, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);
            add(bar, BorderLayout.CENTER);
        }

        void set(int a, int b, String unit) {
            left.setText(a + unit);
            right.setText(b + unit);
            bar.set(a, b);
        }
    }

    private static class Bar extends JComponent {
        private int a;
        private int b;

        Bar() {
            setPreferredSize(new Dimension(100, 6));
        }

        void set(int a, int b) {
            this.a = a;
            this.b = b;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Widgets.smooth(g);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(Theme.TRACK);
            g2.fillRoundRect(0, 0, w, h, h, h);
            int total = a + b;
            if (total > 0) {
                int aw = (int) Math.round(w * a / (double) total);
                g2.setColor(new Color(0x1F2937));
                g2.fillRoundRect(0, 0, Math.max(h, aw), h, h, h);
                g2.setColor(new Color(0xC9CED6));
                g2.fillRoundRect(aw, 0, Math.max(h, w - aw), h, h, h);
            }
            g2.dispose();
        }
    }

    private static class StoneIcon extends JComponent {
        private final Kits.Kit kit;
        private final int r;

        StoneIcon(Kits.Kit kit, int r) {
            this.kit = kit;
            this.r = r;
            setPreferredSize(new Dimension(r * 2 + 4, r * 2 + 4));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Widgets.smooth(g);
            PitchView.drawKitDot(g2, getWidth() / 2, getHeight() / 2 - 1, r, kit);
            g2.dispose();
        }
    }

    private static class FeedRow extends JPanel {
        FeedRow(MatchEvent e, int textWidth, Kits.Match kits) {
            super(new BorderLayout(10, 0));
            boolean goal = e.type() == MatchEvent.Type.GOAL;
            boolean key = e.isKey();
            boolean milestone = e.type() == MatchEvent.Type.KICKOFF || e.type() == MatchEvent.Type.HALFTIME
                    || e.type() == MatchEvent.Type.FULLTIME;
            setOpaque(true);
            setBackground(goal ? Theme.tint(0xFFF4D6, 0x3A3218) : key ? Theme.tint(0xF4F6FA, 0x1F2838)
                    : milestone ? Theme.ACCENT_SOFT : Theme.CARD);
            setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
            setAlignmentX(LEFT_ALIGNMENT);

            String clockText = e.type() == MatchEvent.Type.HALFTIME ? "HT" : e.type() == MatchEvent.Type.FULLTIME ? "FT" : e.clock();
            JLabel minute = Theme.label(clockText, Font.BOLD, 12, goal ? Theme.tint(0xB45309, 0xF59E0B) : Theme.MUTED);
            minute.setPreferredSize(new Dimension(44, 20));
            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            left.setOpaque(false);
            left.add(minute);
            if (e.side() != MatchEvent.Side.NONE) {
                left.add(new StoneIcon(e.side() == MatchEvent.Side.HOME ? kits.home() : kits.away(), key ? 7 : 4));
            } else {
                left.add(Box.createHorizontalStrut(18));
            }
            add(left, BorderLayout.WEST);
            JLabel text = Theme.label("<html><div style='width:" + textWidth + "px'>" + e.text() + "</div></html>", goal || milestone ? Font.BOLD : Font.PLAIN,
                    goal ? 14 : 13, e.type() == MatchEvent.Type.RED ? Theme.BAD : Theme.TEXT);
            add(text, BorderLayout.CENTER);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }
}
