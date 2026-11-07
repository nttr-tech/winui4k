package com.appkitbox.winui4k.swing_to_winui4k.jtogglebutton.basic;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFont;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WToggleButton;
import com.appkitbox.winui4k.WinUiUtilities;

/** 7.2 Turn on and off (WinUI4K). */
public class BasicWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("7.2 Turn on and off (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WLabel preview = new WLabel("Sample text");
            WToggleButton italic = new WToggleButton("Italic");
            italic.addItemListener(e -> preview.setFont(
                    preview.getFont().deriveFont(italic.isSelected() ? WFont.ITALIC : WFont.PLAIN)));

            frame.add(italic);
            frame.add(preview);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            italic.setSelected(true); // turn it on once at startup
        });
    }
}
