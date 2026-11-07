package com.appkitbox.winui4k.swing_to_winui4k.jlabel.basic;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 4.2 Show and change text (WinUI4K). */
public class BasicWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("4.2 Show and change text (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WLabel title = new WLabel("Customers");
            WLabel status = new WLabel();
            int count = 42;
            status.setText("Loaded " + count + " customers");

            frame.add(title);
            frame.add(status);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
