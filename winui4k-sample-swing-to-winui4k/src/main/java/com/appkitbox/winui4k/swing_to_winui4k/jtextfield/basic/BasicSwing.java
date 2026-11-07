package com.appkitbox.winui4k.swing_to_winui4k.jtextfield.basic;

import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** 2.2 Create a text field and read/write its value (Swing). */
public class BasicSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("2.2 Get and Set Text (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JTextField name = new JTextField(20);
            name.setText("Smith");
            JLabel greeting = new JLabel();
            JButton greet = new JButton("Greet");
            greet.addActionListener(e -> greeting.setText("Hello, " + name.getText().trim() + "!"));

            frame.add(name);
            frame.add(greet);
            frame.add(greeting);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
