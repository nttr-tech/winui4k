package com.appkitbox.winui4k.swing_to_winui4k.jbutton.click;

import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/** 1.2 Create a button and respond to clicks (Swing). */
public class ClickSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("1.2 Click (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JLabel message = new JLabel("Not clicked yet");
            JButton button = new JButton("Click me");
            button.addActionListener(e -> message.setText("Clicked!"));

            frame.add(button);
            frame.add(message);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
