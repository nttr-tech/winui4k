package com.appkitbox.winui4k.swing_to_winui4k.jprogressbar.basic;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/** 11.2 Show progress (Swing). */
public class BasicSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("11.2 Progress (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JProgressBar progress = new JProgressBar(0, 100);
            progress.setValue(40);

            frame.add(new JLabel("Copying files..."));
            frame.add(progress);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
