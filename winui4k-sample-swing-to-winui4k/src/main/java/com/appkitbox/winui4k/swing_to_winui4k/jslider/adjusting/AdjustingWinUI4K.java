package com.appkitbox.winui4k.swing_to_winui4k.jslider.adjusting;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WSlider;
import com.appkitbox.winui4k.WinUiUtilities;

/** 10.5 Apply the value when dragging ends (WinUI4K). */
public class AdjustingWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("10.5 While dragging (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WSlider brightness = new WSlider(0, 100, 50);
            brightness.setWidth(200);   // a slider has no default width
            WLabel live = new WLabel("Live: 50");
            WLabel applied = new WLabel("Applied: 50");
            brightness.addChangeListener(e -> {
                live.setText("Live: " + (int) brightness.getValue());
                if (!brightness.getValueIsAdjusting()) {
                    applied.setText("Applied: " + (int) brightness.getValue());
                }
            });

            frame.add(brightness);
            frame.add(live);
            frame.add(applied);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Look as if the user is dragging the knob to 80
            brightness.setValueIsAdjusting(true);
            brightness.setValue(80);
        });
    }
}
