package com.appkitbox.winui4k.swing_to_winui4k.jlist;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WList;
import com.appkitbox.winui4k.WinUiUtilities;

/** 3.7 Selecting and scrolling from the program (WinUI4K). */
public class SelectWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("3.7 Selecting from the program (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            String[] items = new String[50];
            for (int i = 0; i < items.length; i++) {
                items[i] = "Item " + (i + 1);
            }
            WList list = new WList(items);
            list.setWidth(200);
            list.setHeight(150);
            WLabel result = new WLabel("Selected item: none");

            frame.add(list);
            frame.add(result);

            frame.setSize(420, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            list.setSelectedIndex(42);
            list.ensureIndexIsVisible(42);
            result.setText("Selected item: " + list.getSelectedValue());
        });
    }
}
