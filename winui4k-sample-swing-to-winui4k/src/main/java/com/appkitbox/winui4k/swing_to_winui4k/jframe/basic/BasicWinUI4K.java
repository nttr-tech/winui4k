package com.appkitbox.winui4k.swing_to_winui4k.jframe.basic;

import com.appkitbox.winui4k.WDimension;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 24.2 Set the title, size, and location (WinUI4K). */
public class BasicWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("24.2 Title and Size (WinUI4K)");
            frame.setDefaultCloseOperation(WFrame.EXIT_ON_CLOSE);
            frame.setLayout(new WFlowLayout());
            frame.setSize(480, 320);
            frame.setMinimumSize(new WDimension(360, 240));
            frame.setLocationRelativeTo(null);   // center of the screen

            WLabel size = new WLabel("Size: " + frame.getWidth() + " x " + frame.getHeight()
                    + ", title: " + frame.getTitle());
            frame.add(size);

            frame.setVisible(true);
        });
    }
}
