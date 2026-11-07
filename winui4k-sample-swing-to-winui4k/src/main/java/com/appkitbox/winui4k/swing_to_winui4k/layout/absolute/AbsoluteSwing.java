package com.appkitbox.winui4k.swing_to_winui4k.layout.absolute;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/** 30.8 Absolute positioning with a null layout (Swing). */
public class AbsoluteSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("30.8 Absolute positioning (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            frame.setLayout(null);                       // no layout manager: place each child yourself
            JButton start = new JButton("Start");
            start.setBounds(20, 20, 120, 30);            // x, y, width, height
            JLabel note = new JLabel("At (160, 25)");
            note.setBounds(160, 25, 150, 20);
            JButton stop = new JButton("Stop");
            stop.setBounds(60, 80, 200, 50);

            frame.add(start);
            frame.add(note);
            frame.add(stop);

            frame.setSize(360, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
