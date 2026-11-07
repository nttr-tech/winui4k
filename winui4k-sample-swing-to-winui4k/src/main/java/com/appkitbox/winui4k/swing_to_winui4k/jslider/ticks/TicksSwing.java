package com.appkitbox.winui4k.swing_to_winui4k.jslider.ticks;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

/** 10.3 Tick marks and snapping (Swing). */
public class TicksSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("10.3 Ticks (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JSlider level = new JSlider(0, 100, 50);
            level.setMajorTickSpacing(25);
            level.setMinorTickSpacing(5);
            level.setPaintTicks(true);
            level.setPaintLabels(true);
            level.setSnapToTicks(true);

            frame.add(level);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
