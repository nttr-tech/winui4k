package com.appkitbox.winui4k.swing_to_winui4k.jtextfield;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 2.4 Responding to every input (WinUI4K). */
public class ChangeWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("2.4 Responding to every input (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextField title = new WTextField(30);
            WLabel count = new WLabel("0 characters");
            WButton register = new WButton("Register");
            register.setEnabled(false);

            title.addTextChangedListener(text -> {
                count.setText(text.length() + " characters");
                register.setEnabled(!text.isEmpty());
            });

            title.setText("Today's memo");

            frame.add(title);
            frame.add(count);
            frame.add(register);

            frame.setSize(560, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
