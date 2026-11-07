package com.appkitbox.winui4k.swing_to_winui4k.jprogressbar.indeterminate;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/** 11.3 Progress with no known end (Swing). */
public class IndeterminateSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("11.3 Indeterminate (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JProgressBar progress = new JProgressBar();
            progress.setIndeterminate(true);

            frame.add(new JLabel("Connecting..."));
            frame.add(progress);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
