package com.appkitbox.winui4k.swing_to_winui4k.jframe.icon;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 24.4 Set the window icon (WinUI4K). */
public class IconWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("24.4 Icon (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            frame.setIconImage("icons/app.png");
            frame.add(new WLabel("The icon appears in the title bar and the taskbar"));

            frame.setSize(420, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
