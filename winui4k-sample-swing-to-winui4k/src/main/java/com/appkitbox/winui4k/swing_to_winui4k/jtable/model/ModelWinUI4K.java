package com.appkitbox.winui4k.swing_to_winui4k.jtable.model;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTable;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;
import com.appkitbox.winui4k.table.DefaultTableModel;

/** 19.3 Add and remove rows with a model (WinUI4K). */
public class ModelWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("19.3 Table Model (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            DefaultTableModel model = new DefaultTableModel(new Object[] {"Item", "Quantity"}, 0);
            model.addRow(new Object[] {"Milk", 2});
            model.addRow(new Object[] {"Eggs", 12});
            WTable table = new WTable(model);

            WTextField item = new WTextField(12);
            WButton add = new WButton("Add");
            add.addActionListener(e -> {
                if (!item.getText().isEmpty()) {
                    model.addRow(new Object[] {item.getText(), 1});
                    item.setText("");
                }
            });

            WButton remove = new WButton("Delete");
            remove.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row >= 0) model.removeRow(table.convertRowIndexToModel(row));
            });

            table.setWidth(300);
            table.setHeight(180);
            frame.add(item);
            frame.add(add);
            frame.add(remove);
            frame.add(table);

            item.setText("Bread");
            add.doClick();
            table.setRowSelectionInterval(1, 1);

            frame.setSize(460, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
