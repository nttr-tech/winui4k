package com.appkitbox.winui4k.swing_to_winui4k.jtextfield;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** 2.14 Text alignment and appearance (Swing). */
public class AppearanceSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("2.14 Text alignment and appearance (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JTextField price = new JTextField(10);
            price.setHorizontalAlignment(JTextField.RIGHT);
            price.setFont(new Font("Consolas", Font.PLAIN, 14));
            price.setForeground(new Color(0, 90, 158));
            price.setToolTipText("Amount including tax");
            price.setText("1,280");

            frame.add(price);

            frame.setSize(420, 140);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
