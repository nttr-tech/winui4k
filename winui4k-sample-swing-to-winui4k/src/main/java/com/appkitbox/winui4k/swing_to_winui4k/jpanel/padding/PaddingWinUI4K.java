package com.appkitbox.winui4k.swing_to_winui4k.jpanel.padding;

import com.appkitbox.winui4k.WBorderFactory;
import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WGridLayout;
import com.appkitbox.winui4k.WLayoutPanel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 14.5 Leave space inside a panel (WinUI4K). */
public class PaddingWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("14.5 Padding (WinUI4K)");
            frame.setLayout(new WBorderLayout());

            WLayoutPanel content = new WLayoutPanel(new WGridLayout(2, 2, 8, 8));
            content.setBorder(WBorderFactory.createEmptyBorder(24, 24, 24, 24));
            content.setBackground(new WColor(230, 240, 250));
            content.add(new WButton("One"));
            content.add(new WButton("Two"));
            content.add(new WButton("Three"));
            content.add(new WButton("Four"));

            frame.add(content, WBorderLayout.CENTER);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
