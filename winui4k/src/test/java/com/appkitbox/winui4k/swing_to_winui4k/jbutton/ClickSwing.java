package com.appkitbox.winui4k.swing_to_winui4k.jbutton;

import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/** 1.2 Creating a button and responding to clicks (Swing). */
public class ClickSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("1.2 Responding to clicks (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JLabel message = new JLabel("Not pressed yet");
            JButton button = new JButton("Press me");
            button.addActionListener(e -> message.setText("Pressed"));

            frame.add(button);
            frame.add(message);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
