package com.appkitbox.winui4k.swing_to_winui4k.jslider.basic;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

/** 10.2 Slider and its value (Swing). */
public class BasicSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("10.2 Slider (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JSlider volume = new JSlider(0, 100, 30);
            JLabel result = new JLabel("Volume: 30");
            volume.addChangeListener(e -> result.setText("Volume: " + volume.getValue()));

            frame.add(volume);
            frame.add(result);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            volume.setValue(70);
        });
    }
}
