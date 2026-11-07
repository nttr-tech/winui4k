package com.appkitbox.winui4k.swing_to_winui4k.jslider.vertical;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WSlider;
import com.appkitbox.winui4k.WinUiUtilities;

/** 10.4 Vertical and inverted sliders (WinUI4K). */
public class VerticalWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("10.4 Vertical and inverted (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WSlider vertical = new WSlider(WSlider.VERTICAL, 0, 100, 60);
            WSlider inverted = new WSlider(0, 100, 60);
            inverted.setInverted(true);              // the maximum is on the left

            frame.add(vertical);
            frame.add(inverted);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
