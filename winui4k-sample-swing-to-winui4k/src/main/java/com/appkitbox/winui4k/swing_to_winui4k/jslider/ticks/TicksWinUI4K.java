package com.appkitbox.winui4k.swing_to_winui4k.jslider.ticks;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WSlider;
import com.appkitbox.winui4k.WinUiUtilities;

/** 10.3 Tick marks and snapping (WinUI4K). */
public class TicksWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("10.3 Ticks (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WSlider level = new WSlider(0, 100, 50);
            level.setWidth(200);   // a slider has no default width
            level.setMajorTickSpacing(25);
            level.setMinorTickSpacing(5);
            level.setPaintTicks(true);
            // There is no setPaintLabels: the numbers under the ticks are not drawn
            level.setSnapToTicks(true);

            frame.add(level);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
