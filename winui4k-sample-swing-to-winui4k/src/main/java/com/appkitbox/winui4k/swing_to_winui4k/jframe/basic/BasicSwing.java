package com.appkitbox.winui4k.swing_to_winui4k.jframe.basic;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/** 24.2 Set the title, size, and location (Swing). */
public class BasicSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("24.2 Title and Size (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());
            frame.setSize(480, 320);
            frame.setMinimumSize(new Dimension(360, 240));
            frame.setLocationRelativeTo(null);   // center of the screen

            JLabel size = new JLabel("Size: " + frame.getWidth() + " x " + frame.getHeight()
                    + ", title: " + frame.getTitle());
            frame.add(size);

            frame.setVisible(true);
        });
    }
}
