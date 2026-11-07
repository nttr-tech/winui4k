package com.appkitbox.winui4k.swing_to_winui4k.jlabel.alignment;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/** 4.5 Text alignment (Swing). */
public class AlignmentSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("4.5 Text alignment (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JLabel left = new JLabel("Left");                      // LEADING by default
            JLabel center = new JLabel("Center", SwingConstants.CENTER);
            JLabel right = new JLabel("Right");
            right.setHorizontalAlignment(SwingConstants.RIGHT);
            for (JLabel label : new JLabel[] {left, center, right}) {
                label.setPreferredSize(new Dimension(300, 24));
            }

            frame.add(left);
            frame.add(center);
            frame.add(right);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
