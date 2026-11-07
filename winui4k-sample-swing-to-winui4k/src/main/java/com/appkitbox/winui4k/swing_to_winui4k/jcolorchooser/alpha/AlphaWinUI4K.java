package com.appkitbox.winui4k.swing_to_winui4k.jcolorchooser.alpha;

import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WColorPicker;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 28.4 Let the user choose the transparency (WinUI4K). */
public class AlphaWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("28.4 Alpha (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WColorPicker picker = new WColorPicker(new WColor(0, 120, 215, 128));
            picker.setAlphaEnabled(true);   // off by default
            WLabel alpha = new WLabel("Alpha: " + picker.getColor().getAlpha());
            picker.getSelectionModel().addChangeListener(
                e -> alpha.setText("Alpha: " + picker.getColor().getAlpha()));

            frame.add(picker);
            frame.add(alpha);

            frame.setSize(720, 780);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
