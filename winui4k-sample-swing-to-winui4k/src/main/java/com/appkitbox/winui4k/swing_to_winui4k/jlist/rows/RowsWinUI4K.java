package com.appkitbox.winui4k.swing_to_winui4k.jlist.rows;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WList;
import com.appkitbox.winui4k.WListSelectionModel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 3.10 Visible rows and item height (WinUI4K). */
public class RowsWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("3.10 Rows and Height (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            String[] fruits = {"Apple", "Orange", "Grape", "Peach", "Cherry", "Strawberry", "Melon", "Watermelon"};
            WList list = new WList(fruits);
            list.setSelectionMode(WListSelectionModel.SINGLE_SELECTION);

            list.setFixedCellHeight(28);
            list.setVisibleRowCount(5);   // the height becomes 5 rows x 28 + padding

            frame.add(list);

            frame.setSize(420, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
