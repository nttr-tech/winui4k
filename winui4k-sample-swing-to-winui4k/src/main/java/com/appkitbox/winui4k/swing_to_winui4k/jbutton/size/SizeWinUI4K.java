package com.appkitbox.winui4k.swing_to_winui4k.jbutton.size;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WSize;
import com.appkitbox.winui4k.WinUiUtilities;

/** 1.12 Set the size (WinUI4K). */
public class SizeWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("1.12 Size (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WButton ok = new WButton("OK");
            ok.setPreferredSize(new WSize(120, 32));
            // width only
            ok.setWidth(120);

            frame.add(ok);

            frame.setSize(400, 150);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
