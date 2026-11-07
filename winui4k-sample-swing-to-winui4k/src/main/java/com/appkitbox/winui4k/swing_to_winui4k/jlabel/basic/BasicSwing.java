package com.appkitbox.winui4k.swing_to_winui4k.jlabel.basic;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/** 4.2 Show and change text (Swing). */
public class BasicSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("4.2 Show and change text (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JLabel title = new JLabel("Customers");
            JLabel status = new JLabel();
            int count = 42;
            status.setText("Loaded " + count + " customers");

            frame.add(title);
            frame.add(status);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
