package com.appkitbox.winui4k.swing_to_winui4k.jpanel.border;

import com.appkitbox.winui4k.WBorder;
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
            WBorder box = new WBorder(style);       // a container that draws the border
            box.setBorderColor(WColor.GRAY);
            box.setBorderThickness(1);
            box.setPadding(8);

            frame.add(box);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
