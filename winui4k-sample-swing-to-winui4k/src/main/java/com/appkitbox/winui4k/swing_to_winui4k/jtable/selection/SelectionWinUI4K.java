package com.appkitbox.winui4k.swing_to_winui4k.jtable.selection;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WListSelectionModel;
import com.appkitbox.winui4k.WTable;
import com.appkitbox.winui4k.WinUiUtilities;
import com.appkitbox.winui4k.table.DefaultTableModel;

/** 19.5 Read the selected row and open it on double-click (WinUI4K). */
public class SelectionWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("19.5 Row Selection (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            DefaultTableModel model = new DefaultTableModel(new Object[] {"Name", "Department"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;   // read-only, so a double-click does not start editing
                }
            };
            model.addRow(new Object[] {"Alice", "Sales"});
            model.addRow(new Object[] {"Bob", "Engineering"});
            model.addRow(new Object[] {"Carol", "Admin"});
            WTable table = new WTable(model);
            table.setSelectionMode(WListSelectionModel.SINGLE_SELECTION);

            WLabel detail = new WLabel("Select a row");
            table.getSelectionModel().addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) return;
                int row = table.getSelectedRow();
                if (row < 0) {
                    detail.setText("Select a row");
                } else {
                    int modelRow = table.convertRowIndexToModel(row);
                    detail.setText(model.getValueAt(modelRow, 0) + " in " + model.getValueAt(modelRow, 1));
                }
            });

            table.addRowInvokedListener(row -> detail.setText("Opening " + table.getValueAt(row, 0)));

            table.setWidth(300);
            table.setHeight(150);
            frame.add(table);
            frame.add(detail);

            table.setRowSelectionInterval(1, 1);

            frame.setSize(460, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
