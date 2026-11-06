package com.appkitbox.winui4k.swing_to_winui4k.jtextfield;

import java.awt.FlowLayout;
import java.util.Arrays;
import javax.swing.JFrame;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

/** 2.15 JPasswordField → WPasswordField (Swing). */
public class PasswordSwing {
    static void login(char[] value) {
        System.out.println("Log in: " + value.length + " characters");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("2.15 Password field (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JPasswordField password = new JPasswordField(20);
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
