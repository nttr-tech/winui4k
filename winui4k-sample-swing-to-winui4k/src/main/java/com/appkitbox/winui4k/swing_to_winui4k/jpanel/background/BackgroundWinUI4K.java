package com.appkitbox.winui4k.swing_to_winui4k.jpanel.background;

import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WLayoutPanel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 14.4 Paint the background of a panel (WinUI4K). */
public class BackgroundWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("14.4 Background (WinUI4K)");
            frame.setLayout(new WBorderLayout());

            WLayoutPanel header = new WLayoutPanel(new WFlowLayout(WFlowLayout.LEFT));
            header.setBackground(new WColor(0, 90, 158));
            WLabel title = new WLabel("Settings");
            title.setForeground(WColor.WHITE);
            header.add(title);

            frame.add(header, WBorderLayout.NORTH);
            frame.add(new WLabel("Content"), WBorderLayout.CENTER);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
