package com.appkitbox.winui4k.swing_to_winui4k.jslider.basic;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WSlider;
import com.appkitbox.winui4k.WinUiUtilities;

/** 10.2 Slider and its value (WinUI4K). */
public class BasicWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("10.2 Slider (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WSlider volume = new WSlider(0, 100, 30);
            WLabel result = new WLabel("Volume: 30");
            volume.addChangeListener(e -> result.setText("Volume: " + (int) volume.getValue()));

            frame.add(volume);
            frame.add(result);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            volume.setValue(70);
        });
    }
}
