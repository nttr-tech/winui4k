package com.appkitbox.winui4k.swing_to_winui4k.layout.border;

import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WinUiUtilities;

/** 30.3 BorderLayout (WinUI4K). */
public class BorderWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("30.3 BorderLayout (WinUI4K)");

            // the content pane of a WFrame uses WBorderLayout by default
            frame.add(new WButton("North"), WBorderLayout.NORTH);
            frame.add(new WButton("South"), WBorderLayout.SOUTH);
            frame.add(new WButton("West"), WBorderLayout.WEST);
            frame.add(new WButton("East"), WBorderLayout.EAST);
            frame.add(new WButton("Center"));                    // no constraint = CENTER

            frame.setSize(400, 240);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
