package com.appkitbox.winui4k.swing_to_winui4k.jslider.vertical;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

/** 10.4 Vertical and inverted sliders (Swing). */
public class VerticalSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("10.4 Vertical and inverted (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JSlider vertical = new JSlider(JSlider.VERTICAL, 0, 100, 60);
            JSlider inverted = new JSlider(0, 100, 60);
            inverted.setInverted(true);              // the maximum is on the left

            frame.add(vertical);
            frame.add(inverted);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
