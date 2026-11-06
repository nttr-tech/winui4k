package com.appkitbox.winui4k.swing_to_winui4k.jbutton;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 1.2 Creating a button and responding to clicks (WinUI4K). */
public class ClickWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("1.2 Responding to clicks (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WLabel message = new WLabel("Not pressed yet");
            WButton button = new WButton("Press me");
            button.addActionListener(e -> message.setText("Pressed"));

            frame.add(button);
            frame.add(message);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
