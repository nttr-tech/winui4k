package com.appkitbox.winui4k.swing_to_winui4k.jslider.adjusting;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

/** 10.5 Apply the value when dragging ends (Swing). */
public class AdjustingSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("10.5 While dragging (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JSlider brightness = new JSlider(0, 100, 50);
            JLabel live = new JLabel("Live: 50");
            JLabel applied = new JLabel("Applied: 50");
            brightness.addChangeListener(e -> {
                live.setText("Live: " + brightness.getValue());
                if (!brightness.getValueIsAdjusting()) {
                    applied.setText("Applied: " + brightness.getValue());
                }
            });

            frame.add(brightness);
            frame.add(live);
            frame.add(applied);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Look as if the user is dragging the knob to 80
            brightness.setValueIsAdjusting(true);
            brightness.setValue(80);
        });
    }
}
