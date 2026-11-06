package com.appkitbox.winui4k.swing_to_winui4k.jtextfield;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 2.2 Creating a text field and reading and writing its value (WinUI4K). */
public class BasicWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("2.2 Reading and writing the value (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextField name = new WTextField(20);
            name.setText("Smith");
            WLabel greeting = new WLabel();
            WButton greet = new WButton("Greet");
            greet.addActionListener(e -> greeting.setText("Hello, " + name.getText().trim()));

            frame.add(name);
            frame.add(greet);
            frame.add(greeting);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
