package com.appkitbox.winui4k.swing_to_winui4k.jcolorchooser.dialog;

import com.appkitbox.winui4k.WBorder;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WColorPicker;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WSize;
import com.appkitbox.winui4k.WinUiUtilities;

/** 28.2 Choose a color in a dialog (WinUI4K). */
public class DialogWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("28.2 Dialog (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WBorder swatch = new WBorder();
            swatch.setPreferredSize(new WSize(80, 40));
            swatch.setBackground(new WColor(0, 120, 215));
            WButton choose = new WButton("Choose Color...");
            choose.addActionListener(e -> {
                // the owner is a component on the window (a WFrame cannot be passed)
                WColor color = WColorPicker.showDialog(choose, "Choose a color", swatch.getBackground());
                if (color != null) {   // null when canceled
                    swatch.setBackground(color);
                }
            });

            frame.add(choose);
            frame.add(swatch);

            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            choose.doClick();
        });
    }
}
