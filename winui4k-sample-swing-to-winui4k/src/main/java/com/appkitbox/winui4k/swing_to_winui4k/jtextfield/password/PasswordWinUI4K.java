package com.appkitbox.winui4k.swing_to_winui4k.jtextfield.password;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WPasswordField;
import com.appkitbox.winui4k.WinUiUtilities;
import java.util.Arrays;

/** 2.15 JPasswordField -> WPasswordField (WinUI4K). */
public class PasswordWinUI4K {
    static void login(char[] value) {
        System.out.println("Login: " + value.length + " characters");
    }

    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("2.15 Password Field (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WPasswordField password = new WPasswordField(20);
            password.setHeader("Password");
            password.addActionListener(e -> {
                char[] value = password.getPassword();
                try {
                    login(value);
                } finally {
                    Arrays.fill(value, '\0');
                }
            });
            password.setText("secret123");

            frame.add(password);

            frame.setSize(400, 180);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
