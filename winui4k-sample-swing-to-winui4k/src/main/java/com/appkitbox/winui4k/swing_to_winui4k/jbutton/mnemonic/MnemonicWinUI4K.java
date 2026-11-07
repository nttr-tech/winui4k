package com.appkitbox.winui4k.swing_to_winui4k.jbutton.mnemonic;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WinUiUtilities;

/** 1.6 Press with a keyboard mnemonic (WinUI4K). */
public class MnemonicWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("1.6 Mnemonic (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WButton save = new WButton("Save");
            save.setMnemonic('S');            // WKeyEvent.VK_S also works

            frame.add(save);

            frame.setSize(400, 150);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
