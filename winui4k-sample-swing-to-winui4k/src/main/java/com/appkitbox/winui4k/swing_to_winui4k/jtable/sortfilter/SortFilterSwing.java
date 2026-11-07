package com.appkitbox.winui4k.swing_to_winui4k.jtable.sortfilter;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Arrays;
import java.util.regex.Pattern;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/** 19.6 Sort and filter rows (Swing). */
public class SortFilterSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("19.6 Sort and Filter (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            DefaultTableModel model = new DefaultTableModel(new Object[] {"Name", "Department", "Age"}, 0) {
                @Override
                public Class<?> getColumnClass(int column) {
                    return column == 2 ? Integer.class : String.class;   // sort ages as numbers
                }
            };
            model.addRow(new Object[] {"Alice", "Sales", 34});
            model.addRow(new Object[] {"Bob", "Engineering", 28});
            model.addRow(new Object[] {"Carol", "Admin", 45});
            model.addRow(new Object[] {"Dave", "Engineering", 31});
            model.addRow(new Object[] {"Erin", "Engineering", 52});
            JTable table = new JTable(model);

            TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);   // clicking a column header sorts
            sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(2, SortOrder.DESCENDING)));

            JTextField filter = new JTextField(12);
            filter.getDocument().addDocumentListener(new DocumentListener() {
                private void update() {
                    String text = filter.getText();
                    sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    update();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    update();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    update();
                }
            });

            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(360, 180));
            frame.add(filter);
            frame.add(scroll);

            filter.setText("eng");

            frame.setSize(440, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
