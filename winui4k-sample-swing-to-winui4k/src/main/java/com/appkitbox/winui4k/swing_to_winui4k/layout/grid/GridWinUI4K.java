package com.appkitbox.winui4k.swing_to_winui4k.layout.grid;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WGridLayout;
import com.appkitbox.winui4k.WinUiUtilities;

/** 30.4 GridLayout (WinUI4K). */
public class GridWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("30.4 GridLayout (WinUI4K)");

            frame.setLayout(new WGridLayout(0, 3, 5, 5));        // any number of rows, 3 columns
            for (String key : new String[] {"7", "8", "9", "4", "5", "6", "1", "2", "3", "0", ".", "="}) {
                frame.add(new WButton(key));
            }

            frame.setSize(260, 240);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
