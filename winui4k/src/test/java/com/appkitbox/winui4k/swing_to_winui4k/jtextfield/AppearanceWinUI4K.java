package com.appkitbox.winui4k.swing_to_winui4k.jtextfield;

import com.appkitbox.winui4k.TextAlignment;
import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFont;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 2.14 Text alignment and appearance (WinUI4K). */
public class AppearanceWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("2.14 Text alignment and appearance (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextField price = new WTextField(10);
            price.setTextAlignment(TextAlignment.RIGHT);
            price.setFont(new WFont("Consolas", WFont.PLAIN, 14));
            price.setForeground(new WColor(0, 90, 158));
            price.setToolTipText("Amount including tax");
            price.setText("1,280");

            frame.add(price);

            frame.setSize(420, 140);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
