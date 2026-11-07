package com.appkitbox.winui4k.swing_to_winui4k.jscrollpane.basic;

import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WCheckBox;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WScrollPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 15.2 Scroll a panel that is larger than the window (WinUI4K). */
public class BasicWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("15.2 Basic (WinUI4K)");
            frame.setLayout(new WBorderLayout());

            WPanel options = new WPanel();             // a vertical StackPanel
            for (int i = 1; i <= 30; i++) {
                options.add(new WCheckBox("Option " + i));
            }
            WScrollPane scroll = new WScrollPane(options);

            frame.add(scroll, WBorderLayout.CENTER);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
