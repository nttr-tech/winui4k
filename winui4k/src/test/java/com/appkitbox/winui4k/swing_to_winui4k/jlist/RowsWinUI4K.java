package com.appkitbox.winui4k.swing_to_winui4k.jlist;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WList;
import com.appkitbox.winui4k.WinUiUtilities;

/** 3.10 The number of visible rows and the item height (WinUI4K). */
public class RowsWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("3.10 Visible rows and height (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            String[] fruits = {"Apple", "Orange", "Grape", "Peach", "Cherry", "Strawberry", "Melon", "Watermelon"};
            WList list = new WList(fruits);

            list.setFixedCellHeight(28);
            list.setVisibleRowCount(5);   // Sets the height to "5 rows × 28 + padding"

            frame.add(list);

            frame.setSize(420, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
