package superleague.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
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
    private final JList<Integer> slots = new JList<>();
    private final DefaultListModel<Player> benchModel = new DefaultListModel<>();
    private final JList<Player> bench = new JList<>(benchModel);
    private final JButton confirm = Theme.primaryButton("교체");

    private SubstitutionDialog(Component parent, LiveMatch live, Team team, BiConsumer<Integer, Player> onSub) {
        super(SwingUtilities.getWindowAncestor(parent), "선수 교체", ModalityType.APPLICATION_MODAL);
        this.live = live;
        this.team = team;

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
                return p == null ? s.name() + "  (비어 있음)"
                        : String.format("<html><b>%s</b> &nbsp;%s <font color='#64748B'>%s · 능력 %d · 컨디션 %d%%</font></html>",
                        s.name(), p.name(), p.positions(), p.rating(), p.condition());
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
                return String.format("<html><b>%s</b> &nbsp;<font color='#64748B'>%s · 능력 %d · 컨디션 %d%%</font>"
                        + " &nbsp;<font color='%s'>적합 %d%%</font></html>",
                        p.name(), p.positions(), p.rating(), p.condition(), color, fit);
            }
        });
        slots.addListSelectionListener(e -> refreshBench());
        bench.addListSelectionListener(e -> confirm.setEnabled(bench.getSelectedValue() != null));
        confirm.setEnabled(false);

        JPanel lists = new JPanel(new GridLayout(1, 2, 14, 0));
        lists.setOpaque(false);
        lists.add(Theme.card("나갈 선수", scroll(slots)));
        lists.add(Theme.card("들어갈 선수 (이 자리에서 뛸 수 있는 선수, 기대 능력 순)", scroll(bench)));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Theme.label("남은 교체 " + live.subsLeft(team) + " / " + LiveMatch.MAX_SUBS
                + "  ·  교체한 순간부터 남은 시간이 새 선수로 다시 계산됩니다", Font.PLAIN, 12, Theme.MUTED), BorderLayout.WEST);

        JButton cancel = Theme.secondaryButton("취소");
        cancel.addActionListener(e -> dispose());
        confirm.addActionListener(e -> {
            Integer slot = slots.getSelectedValue();
            Player in = bench.getSelectedValue();
            if (slot != null && in != null) {
                dispose();
                onSub.accept(slot, in);
            }
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(confirm);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        root.add(top, BorderLayout.NORTH);
        root.add(lists, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(confirm);
        setSize(920, 520);
        setLocationRelativeTo(parent);
        slots.setSelectedIndex(10);
        slots.ensureIndexIsVisible(10);
    }

    static void show(Component parent, LiveMatch live, Team team, BiConsumer<Integer, Player> onSub) {
        new SubstitutionDialog(parent, live, team, onSub).setVisible(true);
    }

    private Formation.Slot selectedSlot() {
        Integer i = slots.getSelectedValue();
        return i == null ? null : team.slots().get(i);
    }

    private void refreshBench() {
        benchModel.clear();
        Formation.Slot s = selectedSlot();
        Integer i = slots.getSelectedValue();
        if (s == null) {
            return;
        }
        List<Player> options = live.bench(team).stream()
                .filter(p -> live.canSubstitute(team, i, p) && Formation.eligible(p, s))
                .sorted(Comparator.comparingDouble((Player p) -> -p.effectiveRating() * Formation.fit(p, s)))
                .toList();
        options.forEach(benchModel::addElement);
        confirm.setEnabled(false);
    }

    private static JScrollPane scroll(JList<?> list) {
        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setPreferredSize(new Dimension(380, 340));
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
            l.setBackground(selected ? Theme.ACCENT_SOFT : index % 2 == 0 ? Theme.CARD : new Color(0xF8FAFC));
            l.setForeground(Theme.TEXT);
            return l;
        }
    }
}
