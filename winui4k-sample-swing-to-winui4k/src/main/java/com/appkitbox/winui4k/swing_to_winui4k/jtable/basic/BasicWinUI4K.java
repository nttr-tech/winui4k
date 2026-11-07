package com.appkitbox.winui4k.swing_to_winui4k.jtable.basic;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTable;
import com.appkitbox.winui4k.WinUiUtilities;

/** 19.2 Show data in a table (WinUI4K). */
public class BasicWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("19.2 Basic Table (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            String[] columns = {"Name", "Department", "Age"};
            Object[][] data = {
                {"Alice", "Sales", 34},
                {"Bob", "Engineering", 28},
                {"Carol", "Admin", 45},
                {"Dave", "Engineering", 31},
            };
            WTable table = new WTable(data, columns);
            table.setWidth(360);
            table.setHeight(160);
            frame.add(table);

            frame.setSize(440, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
