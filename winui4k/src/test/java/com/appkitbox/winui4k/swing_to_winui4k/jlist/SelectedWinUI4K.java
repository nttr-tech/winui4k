package com.appkitbox.winui4k.swing_to_winui4k.jlist;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WList;
import com.appkitbox.winui4k.WinUiUtilities;

/** 3.4 Reading the selected item (WinUI4K). */
public class SelectedWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("3.4 Reading the selected item (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            String[] fruits = {"Apple", "Orange", "Grape", "Peach", "Cherry"};
            WList list = new WList(fruits);
            list.setWidth(200);
            list.setHeight(150);
            WLabel result = new WLabel("");

            list.setSelectedIndex(1);
            String value = (String) list.getSelectedValue();  // null if nothing is selected
            String text = list.getSelectedItem();             // The toString() string (null if nothing is selected)
            int index = list.getSelectedIndex();              // -1 if nothing is selected
            boolean none = list.isSelectionEmpty();
            result.setText("Selected item: " + value + " (" + text + ") / index: " + index + " / none selected: " + none);

            frame.add(list);
            frame.add(result);

            frame.setSize(420, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
