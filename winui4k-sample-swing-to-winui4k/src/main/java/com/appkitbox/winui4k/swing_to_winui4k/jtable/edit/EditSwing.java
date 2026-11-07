package com.appkitbox.winui4k.swing_to_winui4k.jtable.edit;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

/** 19.7 Edit cells and react to changes (Swing). */
public class EditSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("19.7 Cell Editing (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

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
            JTable table = new JTable(model);

            JComboBox<String> owners = new JComboBox<>(new String[] {"Alice", "Bob", "Carol"});
            table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(owners));

            JLabel status = new JLabel("No changes");
            model.addTableModelListener(e -> {
                if (e.getType() != TableModelEvent.UPDATE || e.getColumn() < 0) return;
                int row = e.getFirstRow();
                status.setText(model.getValueAt(row, 0) + ": " + model.getColumnName(e.getColumn())
                        + " = " + model.getValueAt(row, e.getColumn()));
            });

            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(360, 150));
            frame.add(scroll);
            frame.add(status);

            model.setValueAt(true, 1, 2);

            frame.setSize(440, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
