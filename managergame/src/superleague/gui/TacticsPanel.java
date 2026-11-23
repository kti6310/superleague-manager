package superleague.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import superleague.model.Formation;
import superleague.model.Instructions;
import superleague.model.Player;
import superleague.model.Position;
import superleague.model.TacticalRole;
import superleague.model.Team;

class TacticsPanel extends JPanel {
    private final Team team;
    private final Runnable onChange;
    private final JComboBox<Formation> formationBox = new JComboBox<>(Formation.values());
    private final JCheckBox autoBox = new JCheckBox("매 경기 자동 선발 (체력 반영)");
    private final JButton autoButton = Theme.secondaryButton("지금 자동 선발");
    private final JLabel summary = Theme.label(" ", Font.PLAIN, 12, Theme.MUTED);
    private final JLabel effects = Theme.label(" ", Font.PLAIN, 12, Theme.MUTED);
    private final LineupModel model = new LineupModel();
    private List<Player> bench = List.of();
    private final JTable table;
    private final PitchPanel pitch;
    private final javax.swing.JComboBox<Instructions.Style> styleBox = new javax.swing.JComboBox<>(Instructions.Style.values());
    private final JLabel styleNote = Theme.label("", Font.PLAIN, 12, Theme.MUTED);
    private final JSlider press = slider();
    private final JSlider tempo = slider();
    private final JSlider line = slider();
    private final JSlider width = slider();
    private boolean updating;

    TacticsPanel(Team team, Runnable onChange, JComponent mentality) {
        super(new BorderLayout(12, 12));
        this.team = team;
        this.onChange = onChange;
        setBackground(Theme.CARD);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new BorderLayout(8, 6));
        top.setOpaque(false);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        controls.add(Theme.label("포메이션", Font.BOLD, 13, Theme.TEXT));
        formationBox.setFont(Theme.font(Font.PLAIN, 13));
        formationBox.addActionListener(e -> {
            if (!updating) {
                team.setFormation((Formation) formationBox.getSelectedItem());
                changed();
            }
        });
        controls.add(formationBox);
        autoBox.setOpaque(false);
        autoBox.setFont(Theme.font(Font.PLAIN, 13));
        autoBox.addActionListener(e -> {
            if (!updating) {
                team.setAutoLineup(autoBox.isSelected());
                if (autoBox.isSelected()) {
                    team.autoPick();
                }
                changed();
            }
        });
        controls.add(autoBox);
        autoButton.setPreferredSize(new Dimension(130, 32));
        autoButton.addActionListener(e -> {
            team.autoPick();
            changed();
        });
        controls.add(autoButton);
        top.add(controls, BorderLayout.NORTH);
        top.add(summary, BorderLayout.CENTER);
        JPanel mentalityRow = new JPanel(new BorderLayout(10, 0));
        mentalityRow.setOpaque(false);
        mentalityRow.add(Theme.label("경기 운영", Font.BOLD, 13, Theme.TEXT), BorderLayout.WEST);
        mentalityRow.add(mentality, BorderLayout.CENTER);
        top.add(mentalityRow, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        table = Tables.table(null, row -> false, 1);
        table.setModel(model);
        table.setRowHeight(26);
        javax.swing.table.TableCellRenderer base = table.getDefaultRenderer(Object.class);
        table.setDefaultRenderer(Object.class, (t, value, selected, focus, row, column) -> {
            java.awt.Component c = base.getTableCellRendererComponent(t, value, selected, focus, row, column);
            if (c instanceof JComponent jc && row == 10) {
                jc.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                        javax.swing.BorderFactory.createMatteBorder(0, 0, 3, 0, Theme.TEXT), jc.getBorder()));
            }
            if (row > 10 && !selected) {
                c.setForeground(Theme.MUTED);
            }
            if (c instanceof JComponent jc) {
                TacticalRole duty = row < 11 ? team.duty(slotOfRow(row)) : null;
                jc.setToolTipText(column == 2 && duty != null
                        ? "<html><b>" + duty.label + "</b><br>" + duty.description + "<br><i>"
                        + duty.effectText() + "</i><br>클릭해서 역할 바꾸기</html>" : null);
            }
            if (column == 2 && row < 11 && !selected) {
                c.setForeground(Theme.ACCENT);
            }
            if (column == 0 && row < 11) {
                c.setForeground(Theme.positionColor(team.slots().get(slotOfRow(row)).group()));
            } else if (column == 3) {
                c.setForeground(Theme.positionColor(String.valueOf(value)));
            }
            if ((column == 0 && row < 11) || column == 3) {
                c.setFont(c.getFont().deriveFont(Font.BOLD));
            }
            return c;
        });
        Tables.widths(table, 50, 118, 108, 80, 40, 48, 48, 38, 46);
        table.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        table.setToolTipText("선수 이름을 누르면 세부 능력, 다른 칸을 누르면 이 자리에 넣을 선수를 고를 수 있습니다.");
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 11 && row - 11 < bench.size()) {
                    PlayerCard.show(table, bench.get(row - 11), team.name());
                } else if (row >= 0 && row < 11 && col == 1) {
                    PlayerCard.show(table, team.slotPlayer(slotOfRow(row)), team.name());
                } else if (row >= 0 && row < 11 && table.isEnabled() && col == 2) {
                    java.awt.Rectangle r = table.getCellRect(row, 2, true);
                    openRoleMenu(slotOfRow(row), r.x, r.y + r.height);
                } else if (row >= 0 && row < 11 && table.isEnabled()) {
                    java.awt.Rectangle r = table.getCellRect(row, 1, true);
                    openPicker(slotOfRow(row), table, r.x, r.y + r.height);
                }
            }
        });
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "pick");
        table.getActionMap().put("pick", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int row = table.getSelectedRow();
                if (row >= 0 && row < 11 && table.isEnabled()) {
                    java.awt.Rectangle r = table.getCellRect(row, 1, true);
                    openPicker(slotOfRow(row), table, r.x, r.y + r.height);
                }
            }
        });

        pitch = new PitchPanel((index, point) -> {
            table.getSelectionModel().setSelectionInterval(index, index);
            openPicker(index, pitchComponent(), point.x - 40, point.y + 22);
        });

        JPanel center = new JPanel(new BorderLayout(12, 0));
        center.setOpaque(false);
        JPanel pitchBox = new JPanel(new BorderLayout(0, 4));
        pitchBox.setOpaque(false);
        pitchBox.add(pitch, BorderLayout.CENTER);
        pitchBox.add(Theme.label("<html>다른 선수 위로 끌면 자리 교체 · 빈 점에 놓으면 위치 이동<br>클릭하면 선수 선택</html>", Font.PLAIN, 11, Theme.MUTED), BorderLayout.SOUTH);
        center.add(pitchBox, BorderLayout.WEST);
        center.add(Tables.scroll(table), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        JPanel sliders = new JPanel(new GridLayout(2, 2, 16, 4));
        sliders.setOpaque(false);
        sliders.add(sliderRow("압박 강도", "낮음", "높음", press, v -> team.instructions().setPressing(v)));
        sliders.add(sliderRow("템포", "느리게", "빠르게", tempo, v -> team.instructions().setTempo(v)));
        sliders.add(sliderRow("수비 라인", "내려서기", "끌어올리기", line, v -> team.instructions().setLine(v)));
        sliders.add(sliderRow("공격 폭", "좁게", "넓게", width, v -> team.instructions().setWidth(v)));
        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);
        JPanel styleRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        styleRow.setOpaque(false);
        styleRow.add(Theme.label("팀 지시", Font.BOLD, 13, Theme.TEXT));
        styleRow.add(Theme.label("  전술 스타일", Font.PLAIN, 12, Theme.MUTED));
        styleRow.add(styleBox);
        styleRow.add(styleNote);
        styleBox.addActionListener(e -> {
            Instructions.Style st = (Instructions.Style) styleBox.getSelectedItem();
            if (!updating && st != null && st != Instructions.Style.CUSTOM) {
                team.instructions().set(st.pressing, st.tempo, st.line, st.width);
                changed();
            }
        });
        bottom.add(styleRow, BorderLayout.NORTH);
        bottom.add(sliders, BorderLayout.CENTER);
        bottom.add(effects, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
    }

    private static JSlider slider() {
        JSlider s = new JSlider(Instructions.MIN, Instructions.MAX, Instructions.MID);
        s.setOpaque(false);
        s.setMajorTickSpacing(1);
        s.setPaintTicks(true);
        s.setSnapToTicks(true);
        return s;
    }

    private JPanel sliderRow(String name, String low, String high, JSlider slider, IntConsumer setter) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        JLabel title = Theme.label(name, Font.PLAIN, 13, Theme.TEXT);
        title.setPreferredSize(new Dimension(64, 20));
        row.add(title, BorderLayout.WEST);
        JPanel mid = new JPanel(new BorderLayout(4, 0));
        mid.setOpaque(false);
        mid.add(Theme.label(low, Font.PLAIN, 11, Theme.MUTED), BorderLayout.WEST);
        mid.add(slider, BorderLayout.CENTER);
        mid.add(Theme.label(high, Font.PLAIN, 11, Theme.MUTED), BorderLayout.EAST);
        row.add(mid, BorderLayout.CENTER);
        slider.addChangeListener(e -> {
            if (!updating && !slider.getValueIsAdjusting()) {
                setter.accept(slider.getValue());
                changed();
            }
        });
        return row;
    }

    private JComponent pitchComponent() {
        return pitch;
    }

    private void openPicker(int row, JComponent anchor, int x, int y) {
        Formation.Slot slot = team.slots().get(row);
        java.util.List<Player> options = team.squad().stream()
                .filter(p -> Formation.eligible(p, slot))
                .sorted(Comparator.comparing(Player::isAvailable).reversed()
                        .thenComparing(Comparator.comparingDouble((Player p) -> p.effectiveRating() * Formation.fit(p, slot)).reversed()))
                .toList();
        JList<Player> list = new JList<>(options.toArray(new Player[0]));
        list.setFixedCellHeight(46);
        list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        list.setSelectionBackground(Theme.ACCENT_SOFT);
        list.setSelectedValue(team.slotPlayer(row), true);
        list.setCellRenderer((l, p, index, selected, focus) -> pickerRow(p, slot, selected, p == team.slotPlayer(row)));

        javax.swing.JPopupMenu menu = new javax.swing.JPopupMenu();
        menu.setBorder(BorderFactory.createLineBorder(Theme.BORDER_STRONG));
        JLabel head = Theme.label(String.format("%s 자리에서 뛸 수 있는 선수 %d명 · 이 자리 기대 능력 순", slot.name(), options.size()),
                Font.BOLD, 12, Theme.MUTED);
        head.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        head.setOpaque(true);
        head.setBackground(Theme.SUBTLE);
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(new Dimension(440, Math.min(options.size(), 8) * 46 + 2));
        JPanel box = new JPanel(new BorderLayout());
        box.add(head, BorderLayout.NORTH);
        box.add(scroll, BorderLayout.CENTER);
        menu.add(box);

        Runnable choose = () -> {
            Player chosen = list.getSelectedValue();
            menu.setVisible(false);
            if (chosen != null && chosen != team.slotPlayer(row)) {
                team.setSlot(row, chosen);
                team.setAutoLineup(false);
                changed();
            }
        };
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                int i = list.locationToIndex(e.getPoint());
                if (i >= 0) {
                    list.setSelectedIndex(i);
                    choose.run();
                }
            }
        });
        list.getInputMap().put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "choose");
        list.getActionMap().put("choose", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                choose.run();
            }
        });
        menu.show(anchor, Math.max(0, x), y);
        list.requestFocusInWindow();
    }

    private Component pickerRow(Player p, Formation.Slot slot, boolean selected, boolean current) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        row.setBackground(selected ? Theme.ACCENT_SOFT : Theme.CARD);
        double fit = Formation.fit(p, slot);
        JLabel role = new JLabel(p.positions(), JLabel.CENTER);
        role.setOpaque(true);
        boolean natural = Formation.roleValue(p, slot.label()) >= Formation.SECONDARY_FIT;
        role.setBackground(natural ? Theme.positionColor(p.role().group()) : Theme.TRACK);
        role.setForeground(natural ? Theme.onPositionColor(p.role().group()) : Theme.positionColor(p.role().group()));
        role.setFont(Theme.font(Font.BOLD, 11));
        role.setPreferredSize(new Dimension(Math.max(40, role.getPreferredSize().width + 12), 22));
        JPanel roleWrap = new JPanel(new java.awt.GridBagLayout());
        roleWrap.setOpaque(false);
        roleWrap.add(role);
        row.add(roleWrap, BorderLayout.WEST);
        JPanel text = new JPanel(new GridLayout(2, 1));
        text.setOpaque(false);
        String name = p.name() + (current ? "  (현재)" : "") + (!p.isAvailable() ? "  · 부상 " + p.injuredRounds() + "경기" : "");
        text.add(Theme.label(name, Font.BOLD, 13, p.isAvailable() ? Theme.TEXT : Theme.BAD));
        text.add(Theme.label(String.format("능력 %d · 체력 %d · %s · %dcm", p.rating(), p.condition(),
                p.foot().label(), p.height()), Font.PLAIN, 12, Theme.MUTED));
        row.add(text, BorderLayout.CENTER);
        java.awt.Color fitColor = fit >= 0.999 ? Theme.GOOD : fit >= 0.88 ? Theme.WARN : Theme.BAD;
        JLabel fitLabel = Theme.label(String.format("적합 %.0f%%", fit * 100), Font.BOLD, 12, fitColor);
        row.add(fitLabel, BorderLayout.EAST);
        return row;
    }

    private void refreshBench() {
        java.util.Set<Player> xi = new java.util.HashSet<>(team.startingEleven());
        bench = team.squad().stream().filter(p -> !xi.contains(p))
                .sorted(java.util.Comparator.comparing(Player::position).thenComparing(Player::rating, java.util.Comparator.reverseOrder()))
                .toList();
    }

    private void changed() {
        onChange.run();
    }

    void refresh(boolean editable) {
        updating = true;
        formationBox.setSelectedItem(team.formation());
        autoBox.setSelected(team.isAutoLineup());
        Instructions in = team.instructions();
        press.setValue(in.pressing());
        tempo.setValue(in.tempo());
        line.setValue(in.line());
        width.setValue(in.width());
        Instructions.Style style = Instructions.Style.of(in);
        styleBox.setSelectedItem(style);
        styleNote.setText(style.description);

        refreshBench();
        model.fireTableDataChanged();
        pitch.repaint();

        summary.setText(String.format(
                "<html>선발 전력 <b>%.1f</b> · 평균 적합도 %.0f%% · 포지션 안 맞음 %d명 · 주발 불리 %d명 · 국적 호흡 %d쌍 (%s) · 평균 키 %.0fcm</html>",
                team.lineupStrength(), team.averageFit() * 100, team.roleMismatches(), team.footMismatches(), team.nationalityPairs(),
                Theme.signed(team.chemistryModifier()), team.aerialHeight()));
        effects.setText(String.format(
                "득점 기대 x%.2f · 실점 기대 x%.2f · 경기당 체력 소모 %+d · 공중볼 비중 x%.2f",
                in.attackFactor(), in.concedeFactor(), in.extraFatigue(), in.aerialWeight()));

        formationBox.setEnabled(editable);
        autoBox.setEnabled(editable);
        autoButton.setEnabled(editable);
        table.setEnabled(editable);
        styleBox.setEnabled(editable);
        for (JSlider s : new JSlider[]{press, tempo, line, width}) {
            s.setEnabled(editable);
        }
        updating = false;
    }

    private int slotOfRow(int row) {
        return displayOrder(team).get(row);
    }

    static List<Integer> displayOrder(Team team) {
        List<Formation.Slot> slots = team.slots();
        List<Integer> order = new java.util.ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator.comparingInt((Integer i) -> slots.get(i).group().ordinal())
                .thenComparingInt(i -> lineRank(slots.get(i).label()))
                .thenComparingDouble(i -> -slots.get(i).x()));
        return order;
    }

    private static int lineRank(String label) {
        return switch (label) {
            case "DM" -> 0;
            case "CM", "LM", "RM" -> 1;
            case "AM", "LAM", "RAM" -> 2;
            default -> 0;
        };
    }

    private void openRoleMenu(int row, int x, int y) {
        javax.swing.JPopupMenu menu = new javax.swing.JPopupMenu();
        Formation.Slot slot = team.slots().get(row);
        TacticalRole current = team.duty(row);
        JLabel head = Theme.label(slot.name() + " 자리 역할 선택", Font.BOLD, 12, Theme.MUTED);
        head.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 10, 4, 10));
        menu.add(head);
        for (TacticalRole r : TacticalRole.forSlot(slot.label())) {
            javax.swing.JMenuItem item = new javax.swing.JMenuItem(String.format(
                    "<html><div style='width:330px'><b>%s%s</b><br>%s<br><font color='#2563EB'>%s</font></div></html>",
                    r == current ? "✓ " : "", r.label, r.description, r.effectText()));
            item.setFont(Theme.font(Font.PLAIN, 12));
            item.addActionListener(e -> {
                team.setDuty(row, r);
                changed();
            });
            menu.add(item);
        }
        menu.show(table, x, y);
    }

    private class LineupModel extends AbstractTableModel {
        private final String[] columns = {"자리", "선수", "역할", "포지션", "현재", "체력", "주발", "키", "적합도"};

        @Override
        public int getRowCount() {
            return team.slots().size() + bench.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (row >= 11) {
                Player b = bench.get(row - 11);
                return switch (column) {
                    case 0 -> String.valueOf(row - 10);
                    case 1 -> b.name();
                    case 2 -> "";
                    case 3 -> b.positions();
                    case 4 -> b.rating();
                    case 5 -> b.condition();
                    case 6 -> b.foot().label();
                    case 7 -> b.height();
                    default -> b.injuredRounds() > 0 ? "부상 " + b.injuredRounds() : "후보";
                };
            }
            int index = slotOfRow(row);
            Formation.Slot slot = team.slots().get(index);
            Player p = team.slotPlayer(index);
            if (column == 0) {
                return slot.name();
            }
            if (column == 2) {
                return team.duty(index).label;
            }
            if (p == null) {
                return column == 1 ? "(비어 있음)" : "";
            }
            return switch (column) {
                case 1 -> p.name();
                case 3 -> p.positions();
                case 4 -> p.rating();
                case 5 -> p.condition();
                case 6 -> p.foot().label();
                case 7 -> p.height();
                default -> String.format("%.0f%%", Formation.fit(p, slot) * 100);
            };
        }

    }

    private class PitchPanel extends JPanel {
        private int dragFrom = -1;
        private java.awt.Point dragAt;
        private java.awt.Point pressAt;
        private boolean dragging;

        PitchPanel(java.util.function.BiConsumer<Integer, java.awt.Point> onSlotClick) {
            setPreferredSize(new Dimension(300, 400));
            setOpaque(false);
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    dragFrom = table.isEnabled() ? slotAt(e.getX(), e.getY()) : -1;
                    dragAt = e.getPoint();
                    pressAt = e.getPoint();
                    dragging = false;
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragFrom < 0) {
                        return;
                    }
                    if (!dragging && e.getPoint().distance(pressAt) > 5) {
                        dragging = true;
                        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.MOVE_CURSOR));
                    }
                    dragAt = e.getPoint();
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    int from = dragFrom;
                    boolean wasDrag = dragging;
                    dragFrom = -1;
                    dragging = false;
                    setCursor(java.awt.Cursor.getDefaultCursor());
                    if (from < 0) {
                        return;
                    }
                    if (!wasDrag) {
                        Formation.Slot s = team.slots().get(from);
                        onSlotClick.accept(from, new java.awt.Point(px(s.x()), py(s.y())));
                        return;
                    }
                    int to = slotAt(e.getX(), e.getY());
                    Formation.Slot dot = to < 0 ? dotAt(e.getX(), e.getY(), from) : null;
                    if (canSwap(from, to)) {
                        team.setSlot(to, team.slotPlayer(from));
                        team.setAutoLineup(false);
                        changed();
                    } else if (dot != null) {
                        team.moveSlot(from, dot);
                        changed();
                    }
                    repaint();
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        private int slotAt(int mx, int my) {
            List<Formation.Slot> slots = team.slots();
            for (int i = 0; i < slots.size(); i++) {
                if (Math.hypot(mx - px(slots.get(i).x()), my - py(slots.get(i).y())) < 22) {
                    return i;
                }
            }
            return -1;
        }

        private Formation.Slot dotAt(int mx, int my, int from) {
            if (from < 0 || team.slots().get(from).group() == Position.GK) {
                return null;
            }
            Formation.Slot best = null;
            double bestD = 24;
            for (Formation.Slot d : Formation.DOTS) {
                double dist = Math.hypot(mx - px(d.x()), my - py(d.y()));
                if (dist < bestD && !occupied(d, from)) {
                    bestD = dist;
                    best = d;
                }
            }
            return best;
        }

        private boolean occupied(Formation.Slot dot, int except) {
            List<Formation.Slot> slots = team.slots();
            for (int i = 0; i < slots.size(); i++) {
                if (i != except && Math.hypot(px(slots.get(i).x()) - px(dot.x()), py(slots.get(i).y()) - py(dot.y())) < 30) {
                    return true;
                }
            }
            return false;
        }

        boolean canSwap(int from, int to) {
            if (from < 0 || to < 0 || from == to) {
                return false;
            }
            Player a = team.slotPlayer(from);
            Player b = team.slotPlayer(to);
            return gkOk(to, a) && gkOk(from, b);
        }

        private boolean gkOk(int slot, Player p) {
            boolean gkSlot = team.slots().get(slot).group() == Position.GK;
            return p == null || gkSlot == (p.position() == Position.GK);
        }

        private int px(double x) {
            return (int) Math.round(16 + x * (getWidth() - 32));
        }

        private int py(double y) {
            return (int) Math.round(12 + y * (getHeight() - 40));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(new Color(0x2E7D4F));
            g2.fillRoundRect(0, 0, w, h, 12, 12);
            g2.setColor(new Color(255, 255, 255, 120));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRect(8, 8, w - 16, h - 16);
            g2.drawLine(8, 8, w - 8, 8);
            g2.drawOval(w / 2 - 36, 8 - 36, 72, 72);
            g2.drawRect(w / 2 - 70, h - 8 - 60, 140, 60);
            g2.drawRect(w / 2 - 32, h - 8 - 22, 64, 22);

            List<Formation.Slot> slots = team.slots();
            int target = dragging ? slotAt(dragAt.x, dragAt.y) : -1;
            Formation.Slot dropDot = dragging && target < 0 ? dotAt(dragAt.x, dragAt.y, dragFrom) : null;
            for (Formation.Slot d : Formation.DOTS) {
                if (occupied(d, dragging ? dragFrom : -1)) {
                    continue;
                }
                int dx = px(d.x());
                int dy = py(d.y());
                if (d == dropDot) {
                    g2.setColor(new Color(0xFBBF24));
                    g2.fillOval(dx - 9, dy - 9, 18, 18);
                    g2.setFont(Theme.font(Font.BOLD, 10));
                    g2.drawString(d.name(), dx - g2.getFontMetrics().stringWidth(d.name()) / 2, dy - 12);
                } else {
                    g2.setColor(new Color(255, 255, 255, dragging ? 200 : 110));
                    g2.fillOval(dx - 4, dy - 4, 8, 8);
                }
            }
            for (int i = 0; i < slots.size(); i++) {
                Formation.Slot slot = slots.get(i);
                Player p = dragging && i == dragFrom ? null : team.slotPlayer(i);
                if (dragging && i == target && target != dragFrom) {
                    boolean ok = canSwap(dragFrom, target);
                    g2.setColor(ok ? new Color(0xFBBF24) : Theme.BAD);
                    g2.setStroke(new BasicStroke(3f));
                    g2.drawOval(px(slot.x()) - 23, py(slot.y()) - 23, 46, 46);
                }
                drawToken(g2, px(slot.x()), py(slot.y()), p, slot);
            }
            if (dragging) {
                g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.85f));
                drawToken(g2, dragAt.x, dragAt.y, team.slotPlayer(dragFrom), slots.get(dragFrom));
            }
            g2.dispose();
        }

        private void drawToken(Graphics2D g2, int x, int y, Player p, Formation.Slot slot) {
            {
                double fit = p == null ? 0 : Formation.fit(p, slot);
                Color ring = p == null ? Theme.BAD : fit >= 0.999 ? Color.WHITE : fit >= 0.88 ? Theme.WARN : Theme.BAD;
                g2.setColor(Theme.positionColor(slot.group()));
                g2.fillOval(x - 17, y - 17, 34, 34);
                g2.setColor(ring);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawOval(x - 17, y - 17, 34, 34);
                g2.setFont(Theme.font(Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                String rating = p == null ? "-" : String.valueOf(p.rating());
                g2.setColor(Theme.onPositionColor(slot.group()));
                g2.drawString(rating, x - fm.stringWidth(rating) / 2, y + fm.getAscent() / 2 - 2);
                String name = p == null ? slot.name() : shortName(p.name());
                g2.setFont(Theme.font(Font.BOLD, 11));
                fm = g2.getFontMetrics();
                int tw = fm.stringWidth(name);
                g2.setColor(new Color(0, 0, 0, 120));
                g2.fillRoundRect(x - tw / 2 - 4, y + 19, tw + 8, 16, 6, 6);
                g2.setColor(Color.WHITE);
                g2.drawString(name, x - tw / 2, y + 31);
            }
        }

        private String shortName(String name) {
            String[] parts = name.split(" ");
            String last = parts[parts.length - 1];
            return last.length() > 7 ? last.substring(0, 7) : last;
        }
    }
}
