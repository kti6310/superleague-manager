package superleague.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import superleague.model.Formation;
import superleague.model.Player;
import superleague.model.Team;
import superleague.sim.LiveMatch;

final class SubstitutionDialog extends JDialog {
    private final LiveMatch live;
    private final Team team;
    private final int clock;
    private final Map<Integer, Player> pending = new LinkedHashMap<>();
    private final JList<Integer> slots = new JList<>();
    private final DefaultListModel<Player> benchModel = new DefaultListModel<>();
    private final JList<Player> bench = new JList<>(benchModel);
    private final JLabel pendingLabel = Theme.label(" ", Font.PLAIN, 13, Theme.TEXT);
    private final JButton add = Theme.secondaryButton("교체 목록에 추가 ▼");
    private final JButton clear = Theme.secondaryButton("목록 비우기");
    private final JButton confirm = Theme.primaryButton("교체 확정");

    private SubstitutionDialog(Component parent, LiveMatch live, Team team, int clock, Consumer<Map<Integer, Player>> onSub) {
        super(SwingUtilities.getWindowAncestor(parent), "선수 교체", ModalityType.APPLICATION_MODAL);
        this.live = live;
        this.team = team;
        this.clock = clock;

        DefaultListModel<Integer> slotModel = new DefaultListModel<>();
        for (int i = 0; i < 11; i++) {
            slotModel.addElement(i);
        }
        slots.setModel(slotModel);
        slots.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        slots.setCellRenderer(new Row() {
            @Override
            String text(Object value) {
                int i = (Integer) value;
                Formation.Slot s = team.slots().get(i);
                Player p = team.slotPlayer(i);
                if (p == null) {
                    return s.name() + "  (비어 있음)";
                }
                Player in = pending.get(i);
                String swap = in == null ? "" : " &nbsp;<font color='#2563EB'><b>→ " + in.name() + "</b></font>";
                return String.format("<html><b>%s</b> &nbsp;%s <font color='#64748B'>%s · 능력 %d</font> &nbsp;%s%s</html>",
                        s.name(), p.name(), p.positions(), p.rating(), staminaText(live.stamina(team, p, clock)), swap);
            }
        });
        bench.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bench.setCellRenderer(new Row() {
            @Override
            String text(Object value) {
                Player p = (Player) value;
                Formation.Slot s = selectedSlot();
                int fit = s == null ? 100 : (int) Math.round(Formation.fit(p, s) * 100);
                String color = fit >= 100 ? "#16A34A" : fit >= 88 ? "#D97706" : "#DC2626";
                return String.format("<html><b>%s</b> &nbsp;<font color='#64748B'>%s · 능력 %d</font> &nbsp;%s"
                                + " &nbsp;<font color='%s'>적합 %d%%</font></html>",
                        p.name(), p.positions(), p.rating(), staminaText(p.condition()), color, fit);
            }
        });
        slots.addListSelectionListener(e -> refreshBench());
        bench.addListSelectionListener(e -> updateButtons());

        JPanel lists = new JPanel(new GridLayout(1, 2, 14, 0));
        lists.setOpaque(false);
        lists.add(Theme.card("나갈 선수 (현재 체력)", scroll(slots)));
        lists.add(Theme.card("들어갈 선수 (이 자리에서 뛸 수 있는 선수, 기대 능력 순)", scroll(bench)));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Theme.label(String.format("남은 교체 %d명 / %d명 · 남은 교체 기회 %d번 / %d번  ·  한 번에 여러 명을 골라 함께 교체할 수 있습니다",
                live.subsLeft(team), LiveMatch.MAX_SUBS, live.windowsLeft(team), LiveMatch.MAX_WINDOWS),
                Font.PLAIN, 12, Theme.MUTED), BorderLayout.WEST);

        add.addActionListener(e -> {
            Integer slot = slots.getSelectedValue();
            Player in = bench.getSelectedValue();
            if (slot != null && in != null) {
                pending.put(slot, in);
                refreshPending();
            }
        });
        clear.addActionListener(e -> {
            pending.clear();
            refreshPending();
        });
        JButton cancel = Theme.secondaryButton("취소");
        cancel.addActionListener(e -> dispose());
        confirm.addActionListener(e -> {
            if (!pending.isEmpty()) {
                dispose();
                onSub.accept(new LinkedHashMap<>(pending));
            }
        });

        JPanel pendingRow = new JPanel(new BorderLayout(10, 0));
        pendingRow.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(add);
        left.add(clear);
        pendingRow.add(left, BorderLayout.WEST);
        pendingRow.add(pendingLabel, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(confirm);

        JPanel south = new JPanel(new GridLayout(2, 1, 0, 10));
        south.setOpaque(false);
        south.add(pendingRow);
        south.add(buttons);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        root.add(top, BorderLayout.NORTH);
        root.add(lists, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(confirm);
        setSize(1000, 600);
        setLocationRelativeTo(parent);
        refreshPending();
        slots.setSelectedIndex(10);
        slots.ensureIndexIsVisible(10);
    }

    static void show(Component parent, LiveMatch live, Team team, int clock, Consumer<Map<Integer, Player>> onSub) {
        new SubstitutionDialog(parent, live, team, clock, onSub).setVisible(true);
    }

    static String staminaText(int stamina) {
        String color = stamina >= 75 ? "#16A34A" : stamina >= 55 ? "#D97706" : "#DC2626";
        return String.format("<font color='%s'>체력 %d%%</font>", color, stamina);
    }

    private Formation.Slot selectedSlot() {
        Integer i = slots.getSelectedValue();
        return i == null ? null : team.slots().get(i);
    }

    private void refreshPending() {
        StringBuilder sb = new StringBuilder("<html>");
        if (pending.isEmpty()) {
            sb.append("<font color='#64748B'>나갈 선수와 들어갈 선수를 고르고 \"교체 목록에 추가\"를 누르세요</font>");
        } else {
            sb.append("<b>교체 목록 ").append(pending.size()).append("명:</b> ");
            pending.forEach((slot, in) -> sb.append(team.slotPlayer(slot).name()).append(" → ").append(in.name()).append(" &nbsp; "));
        }
        pendingLabel.setText(sb.append("</html>").toString());
        confirm.setText(pending.isEmpty() ? "교체 확정" : "교체 확정 (" + pending.size() + "명)");
        slots.repaint();
        refreshBench();
    }

    private void refreshBench() {
        benchModel.clear();
        Formation.Slot s = selectedSlot();
        Integer i = slots.getSelectedValue();
        if (s != null) {
            List<Player> options = live.bench(team).stream()
                    .filter(p -> !pending.containsValue(p) || p == pending.get(i))
                    .filter(p -> live.canSubstitute(team, i, p) && Formation.eligible(p, s))
                    .sorted(Comparator.comparingDouble((Player p) -> -p.effectiveRating() * Formation.fit(p, s)))
                    .toList();
            options.forEach(benchModel::addElement);
        }
        updateButtons();
    }

    private void updateButtons() {
        Integer slot = slots.getSelectedValue();
        boolean room = pending.size() < live.subsLeft(team) || (slot != null && pending.containsKey(slot));
        add.setEnabled(room && slot != null && bench.getSelectedValue() != null);
        clear.setEnabled(!pending.isEmpty());
        confirm.setEnabled(!pending.isEmpty());
    }

    private static JScrollPane scroll(JList<?> list) {
        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setPreferredSize(new Dimension(420, 340));
        return sp;
    }

    private abstract static class Row extends DefaultListCellRenderer {
        abstract String text(Object value);

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                                                      boolean focus) {
            JLabel l = (JLabel) super.getListCellRendererComponent(list, text(value), index, selected, false);
            l.setFont(Theme.font(Font.PLAIN, 13));
            l.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            l.setBackground(selected ? Theme.ACCENT_SOFT : index % 2 == 0 ? Theme.CARD : Theme.ROW_ALT);
            l.setForeground(Theme.TEXT);
            return l;
        }
    }
}
