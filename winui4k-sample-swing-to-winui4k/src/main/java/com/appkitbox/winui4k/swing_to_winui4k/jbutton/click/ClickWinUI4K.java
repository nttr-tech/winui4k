package com.appkitbox.winui4k.swing_to_winui4k.jbutton.click;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 1.2 Create a button and respond to clicks (WinUI4K). */
public class ClickWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("1.2 Click (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WLabel message = new WLabel("Not clicked yet");
            WButton button = new WButton("Click me");
            button.addActionListener(e -> message.setText("Clicked!"));

            frame.add(button);
            frame.add(message);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
