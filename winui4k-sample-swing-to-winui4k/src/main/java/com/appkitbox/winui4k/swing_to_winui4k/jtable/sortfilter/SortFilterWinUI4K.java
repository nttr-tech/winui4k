package com.appkitbox.winui4k.swing_to_winui4k.jtable.sortfilter;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTable;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;
import com.appkitbox.winui4k.table.DefaultTableModel;
import com.appkitbox.winui4k.table.RowFilter;
import com.appkitbox.winui4k.table.RowSorter;
import com.appkitbox.winui4k.table.SortOrder;
import com.appkitbox.winui4k.table.TableModel;
import com.appkitbox.winui4k.table.TableRowSorter;
import java.util.Arrays;
import java.util.regex.Pattern;

/** 19.6 Sort and filter rows (WinUI4K). */
public class SortFilterWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("19.6 Sort and Filter (WinUI4K)");
            frame.setLayout(new WFlowLayout());

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
            WTable table = new WTable(model);

            TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);   // clicking a column header sorts
            sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(2, SortOrder.DESCENDING)));

            WTextField filter = new WTextField(12);
            filter.addTextChangedListener(text ->
                sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(text))));

            table.setWidth(360);
            table.setHeight(180);
            frame.add(filter);
            frame.add(table);

            filter.setText("eng");

            frame.setSize(440, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
