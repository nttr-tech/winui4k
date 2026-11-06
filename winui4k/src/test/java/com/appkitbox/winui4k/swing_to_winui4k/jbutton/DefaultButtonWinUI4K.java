package com.appkitbox.winui4k.swing_to_winui4k.jbutton;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WinUiUtilities;

/** 1.7 A default button pressed with Enter (WinUI4K). */
public class DefaultButtonWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("1.7 Default button (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WButton ok = new WButton("OK");
            WButton cancel = new WButton("Cancel");
            frame.setDefaultButton(ok);

            frame.add(ok);
            frame.add(cancel);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
