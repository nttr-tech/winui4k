package com.appkitbox.winui4k.swing_to_winui4k.jtextfield;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 2.7 Making it read-only (WinUI4K). */
public class ReadOnlyWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("2.7 Read-only (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextField id = new WTextField(10);
            id.setText("A-0001");
            id.setEditable(false);

            frame.add(id);

            frame.setSize(420, 140);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
