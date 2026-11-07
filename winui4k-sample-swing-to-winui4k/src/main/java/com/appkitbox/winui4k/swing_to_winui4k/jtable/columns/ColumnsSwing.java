package com.appkitbox.winui4k.swing_to_winui4k.jtable.columns;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** 19.4 Column types, widths and renderers (Swing). */
public class ColumnsSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("19.4 Columns (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            DefaultTableModel model = new DefaultTableModel(new Object[] {"Product", "Stock", "Price", "On sale"}, 0) {
                @Override
                public Class<?> getColumnClass(int column) {
                    switch (column) {
                        case 1: return Integer.class;   // numbers are right-aligned
                        case 2: return Double.class;
                        case 3: return Boolean.class;   // shown as a check box
                        default: return String.class;
                    }
                }
            };
            model.addRow(new Object[] {"Apple", 12, 1.5, true});
            model.addRow(new Object[] {"Orange", 30, 0.8, false});
            model.addRow(new Object[] {"Grape", 0, 4.8, false});
            JTable table = new JTable(model);

            table.getColumnModel().getColumn(0).setPreferredWidth(140);
            table.getColumnModel().getColumn(1).setPreferredWidth(70);
            table.getColumnModel().getColumn(2).setPreferredWidth(90);

            DefaultTableCellRenderer priceRenderer = new DefaultTableCellRenderer() {
                @Override
                protected void setValue(Object value) {
                    setText(String.format("$%.2f", (Double) value));
                }
            };
            priceRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
            table.getColumnModel().getColumn(2).setCellRenderer(priceRenderer);

            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(400, 180));
            frame.add(scroll);

            frame.setSize(480, 280);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
