package com.appkitbox.winui4k.swing_to_winui4k.jtable.columns;

import com.appkitbox.winui4k.HorizontalAlignment;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTable;
import com.appkitbox.winui4k.WinUiUtilities;
import com.appkitbox.winui4k.table.DefaultTableCellRenderer;
import com.appkitbox.winui4k.table.DefaultTableModel;

/** 19.4 Column types, widths and renderers (WinUI4K). */
public class ColumnsWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("19.4 Columns (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            DefaultTableModel model = new DefaultTableModel(new Object[] {"Product", "Stock", "Price", "On sale"}, 0) {
                @Override
                public Class<?> getColumnClass(int column) {
                    switch (column) {
                        case 1: return Integer.class;
                        case 2: return Double.class;
                        case 3: return Boolean.class;   // shown as a check box
                        default: return String.class;
                    }
                }
            };
            model.addRow(new Object[] {"Apple", 12, 1.5, true});
            model.addRow(new Object[] {"Orange", 30, 0.8, false});
            model.addRow(new Object[] {"Grape", 0, 4.8, false});
            WTable table = new WTable(model);

            table.getColumnModel().getColumn(0).setPreferredWidth(140);
            table.getColumnModel().getColumn(1).setPreferredWidth(70);
            table.getColumnModel().getColumn(2).setPreferredWidth(90);

            DefaultTableCellRenderer priceRenderer = new DefaultTableCellRenderer() {
                @Override
                protected String getText(Object value) {
                    return String.format("$%.2f", (Double) value);
                }
            };
            priceRenderer.setHorizontalAlignment(HorizontalAlignment.RIGHT);
            table.getColumnModel().getColumn(2).setCellRenderer(priceRenderer);

            table.setWidth(400);
            table.setHeight(180);
            frame.add(table);

            frame.setSize(480, 280);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
