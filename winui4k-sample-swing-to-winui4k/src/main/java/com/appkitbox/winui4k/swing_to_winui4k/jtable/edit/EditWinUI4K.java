package com.appkitbox.winui4k.swing_to_winui4k.jtable.edit;

import com.appkitbox.winui4k.WComboBox;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WTable;
import com.appkitbox.winui4k.WinUiUtilities;
import com.appkitbox.winui4k.table.DefaultCellEditor;
import com.appkitbox.winui4k.table.DefaultTableModel;
import com.appkitbox.winui4k.table.TableModelEvent;

/** 19.7 Edit cells and react to changes (WinUI4K). */
public class EditWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("19.7 Cell Editing (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            DefaultTableModel model = new DefaultTableModel(new Object[] {"Task", "Owner", "Done"}, 0) {
                @Override
                public Class<?> getColumnClass(int column) {
                    return column == 2 ? Boolean.class : String.class;
                }

                @Override
                public boolean isCellEditable(int row, int column) {
                    return column != 0;   // the task name is read-only
                }
            };
            model.addRow(new Object[] {"Write the spec", "Alice", true});
            model.addRow(new Object[] {"Review the code", "Bob", false});
            model.addRow(new Object[] {"Release", "Carol", false});
            WTable table = new WTable(model);

            WComboBox owners = new WComboBox(new String[] {"Alice", "Bob", "Carol"});
            table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(owners));

            WLabel status = new WLabel("No changes");
            model.addTableModelListener(e -> {
                if (e.getType() != TableModelEvent.UPDATE || e.getColumn() < 0) return;
                int row = e.getFirstRow();
                status.setText(model.getValueAt(row, 0) + ": " + model.getColumnName(e.getColumn())
                        + " = " + model.getValueAt(row, e.getColumn()));
            });

            table.setWidth(360);
            table.setHeight(150);
            frame.add(table);
            frame.add(status);

            model.setValueAt(true, 1, 2);

            frame.setSize(440, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
