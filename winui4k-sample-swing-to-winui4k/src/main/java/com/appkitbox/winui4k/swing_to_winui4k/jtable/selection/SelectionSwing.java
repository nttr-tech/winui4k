package com.appkitbox.winui4k.swing_to_winui4k.jtable.selection;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/** 19.5 Read the selected row and open it on double-click (Swing). */
public class SelectionSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("19.5 Row Selection (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            DefaultTableModel model = new DefaultTableModel(new Object[] {"Name", "Department"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;   // read-only, so a double-click does not start editing
                }
            };
            model.addRow(new Object[] {"Alice", "Sales"});
            model.addRow(new Object[] {"Bob", "Engineering"});
            model.addRow(new Object[] {"Carol", "Admin"});
            JTable table = new JTable(model);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            JLabel detail = new JLabel("Select a row");
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

            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() != 2) return;
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) detail.setText("Opening " + table.getValueAt(row, 0));
                }
            });

            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(300, 150));
            frame.add(scroll);
            frame.add(detail);

            table.setRowSelectionInterval(1, 1);

            frame.setSize(460, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
