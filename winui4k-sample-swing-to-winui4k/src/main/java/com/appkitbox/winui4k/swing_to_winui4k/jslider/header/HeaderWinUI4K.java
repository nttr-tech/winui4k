package com.appkitbox.winui4k.swing_to_winui4k.jslider.header;

import com.appkitbox.winui4k.TickPlacement;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WSlider;
import com.appkitbox.winui4k.WinUiUtilities;

/** 10.6 Header, decimal steps and tick placement (WinUI4K). */
public class HeaderWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("10.6 Header and steps (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WSlider opacity = new WSlider(0, 1, 0.6);         // a decimal range
            opacity.setHeader("Opacity");
            opacity.setStepFrequency(0.1);
            opacity.setTickFrequency(0.25);
            opacity.setTickPlacement(TickPlacement.OUTSIDE);
            opacity.setWidth(240);

            frame.add(opacity);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
