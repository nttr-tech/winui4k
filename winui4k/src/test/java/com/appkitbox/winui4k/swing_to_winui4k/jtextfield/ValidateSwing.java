package com.appkitbox.winui4k.swing_to_winui4k.jtextfield;

import java.awt.FlowLayout;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** 2.10 Validating when the focus leaves (Swing). */
public class ValidateSwing {
    static boolean isValidAge(String text) {
        try {
            int value = Integer.parseInt(text.trim());
            return 0 <= value && value <= 150;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("2.10 Validating when the focus leaves (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JTextField age = new JTextField(4);
            age.setInputVerifier(new InputVerifier() {
                @Override
                public boolean verify(JComponent input) {
                    return isValidAge(((JTextField) input).getText());   // If false, the focus does not leave
                }
            });
            age.setText("200");

            frame.add(age);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
