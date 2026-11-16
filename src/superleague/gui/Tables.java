package superleague.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.function.IntPredicate;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

final class Tables {
    private Tables() {
    }

    static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    static JTable table(DefaultTableModel model, IntPredicate highlightRow, int... leftAlignedColumns) {
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new java.awt.Dimension(0, 1));
        table.setGridColor(new Color(0xEEF1F5));
        table.setFont(Theme.font(Font.PLAIN, 13));
        table.setSelectionBackground(Theme.ACCENT_SOFT);
        table.setSelectionForeground(Theme.TEXT);
        table.setFillsViewportHeight(true);
        table.setBackground(Theme.CARD);
        table.getTableHeader().setReorderingAllowed(false);

        JTableHeader header = table.getTableHeader();
        header.setFont(Theme.font(Font.BOLD, 12));
        header.setBackground(Theme.CARD);
        header.setForeground(Theme.MUTED);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        header.setPreferredSize(new java.awt.Dimension(header.getPreferredSize().width, 34));
        DefaultTableCellRenderer headRenderer = new DefaultTableCellRenderer();
        headRenderer.setHorizontalAlignment(JLabel.CENTER);
        header.setDefaultRenderer((t, value, selected, focus, row, column) -> {
            JLabel h = (JLabel) headRenderer.getTableCellRendererComponent(t, value, false, false, row, column);
            h.setFont(Theme.font(Font.BOLD, 12));
            h.setForeground(Theme.MUTED);
            h.setBackground(Theme.CARD);
            h.setOpaque(true);
            boolean left = false;
            for (int col : leftAlignedColumns) {
                left |= col == column;
            }
            h.setHorizontalAlignment(left ? JLabel.LEFT : JLabel.CENTER);
            h.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                    BorderFactory.createEmptyBorder(0, 8, 0, 8)));
            return h;
        });

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(t, value, selected, false, row, column);
                boolean left = false;
                for (int col : leftAlignedColumns) {
                    if (col == column) {
                        left = true;
                    }
                }
                c.setHorizontalAlignment(left ? JLabel.LEFT : JLabel.CENTER);
                c.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                boolean mine = highlightRow != null && highlightRow.test(row);
                c.setFont(Theme.font(mine ? Font.BOLD : Font.PLAIN, 13));
                if (!selected) {
                    c.setBackground(mine ? Theme.ACCENT_SOFT : row % 2 == 1 ? Theme.ZEBRA : Theme.CARD);
                }
                c.setForeground(colorFor(value, mine));
                return c;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);
        return table;
    }

    private static Color colorFor(Object value, boolean mine) {
        if (value instanceof String s) {
            switch (s) {
                case "승":
                    return Theme.GOOD;
                case "패":
                    return Theme.BAD;
                case "무":
                    return Theme.MUTED;
                default:
                    if (s.startsWith("부상")) {
                        return Theme.BAD;
                    }
            }
        }
        return mine ? Theme.ACCENT_PRESS : Theme.TEXT;
    }

    static JScrollPane scroll(JTable table) {
        JScrollPane pane = new JScrollPane(table);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.getViewport().setBackground(Theme.CARD);
        return pane;
    }

    static void widths(JTable table, int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }
}
