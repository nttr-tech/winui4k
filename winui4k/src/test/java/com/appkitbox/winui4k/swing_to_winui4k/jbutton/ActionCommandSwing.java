package com.appkitbox.winui4k.swing_to_winui4k.jbutton;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** 1.9 Handling several buttons with one listener (Swing). */
public class ActionCommandSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("1.9 ActionCommand (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JTextField display = new JTextField(10);
            ActionListener append = e -> display.setText(display.getText() + e.getActionCommand());

            JPanel keys = new JPanel(new GridLayout(0, 3, 4, 4));
            for (int i = 1; i <= 9; i++) {
                JButton key = new JButton(String.valueOf(i));
                key.addActionListener(append);      // actionCommand is the button text by default
                keys.add(key);
            }

            frame.add(display);
            frame.add(keys);

            frame.setSize(400, 350);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
