package com.appkitbox.winui4k.swing_to_winui4k.jtable.model;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/** 19.3 Add and remove rows with a model (Swing). */
public class ModelSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("19.3 Table Model (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            DefaultTableModel model = new DefaultTableModel(new Object[] {"Item", "Quantity"}, 0);
            model.addRow(new Object[] {"Milk", 2});
            model.addRow(new Object[] {"Eggs", 12});
            JTable table = new JTable(model);

            JTextField item = new JTextField(12);
            JButton add = new JButton("Add");
            add.addActionListener(e -> {
                if (!item.getText().isEmpty()) {
                    model.addRow(new Object[] {item.getText(), 1});
                    item.setText("");
                }
            });

            JButton remove = new JButton("Delete");
            remove.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row >= 0) model.removeRow(table.convertRowIndexToModel(row));
            });

            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(300, 180));
            frame.add(item);
            frame.add(add);
            frame.add(remove);
            frame.add(scroll);

            item.setText("Bread");
            add.doClick();
            table.setRowSelectionInterval(1, 1);

            frame.setSize(460, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
