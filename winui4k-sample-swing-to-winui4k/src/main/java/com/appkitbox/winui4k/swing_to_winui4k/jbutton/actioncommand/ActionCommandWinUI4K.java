package com.appkitbox.winui4k.swing_to_winui4k.jbutton.actioncommand;

import com.appkitbox.winui4k.WActionListener;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WGridLayout;
import com.appkitbox.winui4k.WLayoutPanel;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 1.9 Handle many buttons with one listener (WinUI4K). */
public class ActionCommandWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("1.9 Action Command (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextField display = new WTextField(10);
            WActionListener append = e -> display.setText(display.getText() + e.getActionCommand());

            WLayoutPanel keys = new WLayoutPanel(new WGridLayout(0, 3, 4, 4));
            for (int i = 1; i <= 9; i++) {
                WButton key = new WButton(String.valueOf(i));
                key.addActionListener(append);      // the action command defaults to the button text
                keys.add(key);
            }

            frame.add(display);
            frame.add(keys);

            frame.setSize(400, 350);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
