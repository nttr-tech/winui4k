package com.appkitbox.winui4k.swing_to_winui4k.jpanel.border;

import com.appkitbox.winui4k.WBorderFactory;
import com.appkitbox.winui4k.WCheckBox;
import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WGridLayout;
import com.appkitbox.winui4k.WLayoutPanel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 14.3 Draw a border around a panel (WinUI4K). */
public class BorderWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("14.3 Border (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WLayoutPanel style = new WLayoutPanel(new WGridLayout(0, 1));
            style.add(new WCheckBox("Bold"));
            style.add(new WCheckBox("Italic"));
            style.add(new WCheckBox("Underline"));
            style.setBorder(WBorderFactory.createCompoundBorder(
                    WBorderFactory.createLineBorder(WColor.GRAY, 1),
                    WBorderFactory.createEmptyBorder(8, 8, 8, 8)));

            frame.add(style);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
