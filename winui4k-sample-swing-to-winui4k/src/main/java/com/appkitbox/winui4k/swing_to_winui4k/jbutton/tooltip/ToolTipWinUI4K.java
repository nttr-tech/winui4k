package com.appkitbox.winui4k.swing_to_winui4k.jbutton.tooltip;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WinUiUtilities;

/** 1.5 Show a tooltip (WinUI4K). */
public class ToolTipWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("1.5 ToolTip (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WButton refresh = new WButton("Refresh");
            refresh.setToolTipText("Refresh the list (F5)");

            frame.add(refresh);

            frame.setSize(400, 150);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
