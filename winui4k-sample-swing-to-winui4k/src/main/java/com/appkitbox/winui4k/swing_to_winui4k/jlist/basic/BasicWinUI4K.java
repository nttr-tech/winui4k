package com.appkitbox.winui4k.swing_to_winui4k.jlist.basic;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WList;
import com.appkitbox.winui4k.WinUiUtilities;

/** 3.2 Show fixed items (WinUI4K). */
public class BasicWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("3.2 Fixed Items (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            String[] fruits = {"Apple", "Orange", "Grape", "Peach", "Cherry"};
            WList list = new WList(fruits);           // single selection by default
            list.setWidth(200);
            list.setHeight(150);
            frame.add(list);

            frame.setSize(420, 280);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
