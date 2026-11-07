package com.appkitbox.winui4k.swing_to_winui4k.layout.flow;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WinUiUtilities;

/** 30.2 FlowLayout (WinUI4K). */
public class FlowWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("30.2 FlowLayout (WinUI4K)");

            frame.setLayout(new WFlowLayout(WFlowLayout.LEFT, 10, 10));
            for (String name : new String[] {"One", "Two", "Three", "Four", "Five", "Six"}) {
                frame.add(new WButton(name));
            }

            frame.setSize(320, 180);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
