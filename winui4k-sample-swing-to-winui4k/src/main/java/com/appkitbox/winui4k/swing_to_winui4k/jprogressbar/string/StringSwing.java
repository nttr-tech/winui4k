package com.appkitbox.winui4k.swing_to_winui4k.jprogressbar.string;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/** 11.4 Show the percentage (Swing). */
public class StringSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("11.4 Percentage (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JProgressBar progress = new JProgressBar(0, 100);
            progress.setValue(65);
            progress.setStringPainted(true);       // draws "65%" on the bar

            frame.add(progress);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
