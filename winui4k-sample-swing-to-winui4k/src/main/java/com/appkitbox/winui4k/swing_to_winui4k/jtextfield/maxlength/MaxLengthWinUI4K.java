package com.appkitbox.winui4k.swing_to_winui4k.jtextfield.maxlength;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 2.6 Limit the length (WinUI4K). */
public class MaxLengthWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("2.6 Max Length (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextField code = new WTextField(12);
            code.setMaxLength(10);   // 0 means no limit
            code.setText("0123456789");

            frame.add(code);

            frame.setSize(420, 140);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
