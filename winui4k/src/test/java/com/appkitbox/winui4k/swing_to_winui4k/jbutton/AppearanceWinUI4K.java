package com.appkitbox.winui4k.swing_to_winui4k.jbutton;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFont;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WinUiUtilities;

/** 1.11 Fonts, colors and flat buttons (WinUI4K). */
public class AppearanceWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("1.11 Appearance (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WButton danger = new WButton("Delete");
            danger.setFont(new WFont("Yu Gothic UI", WFont.BOLD, 14));
            danger.setForeground(WColor.WHITE);
            danger.setBackground(new WColor(196, 43, 28));

            WButton link = new WButton("Learn more");
            link.setContentAreaFilled(false);
            link.setBorderPainted(false);
            link.setFocusPainted(false);

            frame.add(danger);
            frame.add(link);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
