package com.appkitbox.winui4k.swing_to_winui4k.jlist.selected;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WList;
import com.appkitbox.winui4k.WinUiUtilities;

/** 3.4 Read the selected item (WinUI4K). */
public class SelectedWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("3.4 Read Selection (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            String[] fruits = {"Apple", "Orange", "Grape", "Peach", "Cherry"};
            WList list = new WList(fruits);
            list.setWidth(200);
            list.setHeight(150);
            WLabel result = new WLabel("");

            list.setSelectedIndex(1);
            String value = (String) list.getSelectedValue();  // null if nothing is selected
            String text = list.getSelectedItem();             // toString() text (null if nothing is selected)
            int index = list.getSelectedIndex();              // -1 if nothing is selected
            boolean none = list.isSelectionEmpty();
            result.setText("Selected: " + value + " (" + text + ") / index: " + index + " / none: " + none);

            frame.add(list);
            frame.add(result);

            frame.setSize(420, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
