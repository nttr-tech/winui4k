package com.appkitbox.winui4k.swing_to_winui4k.jprogressbar.model;

import java.awt.FlowLayout;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

/** 11.6 Share a model with a slider (Swing). */
public class ModelSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("11.6 Shared model (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            BoundedRangeModel model = new DefaultBoundedRangeModel(30, 0, 0, 100);
            JSlider slider = new JSlider(model);
            JProgressBar progress = new JProgressBar(model);

            frame.add(slider);
            frame.add(progress);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            model.setValue(75);
        });
    }
}
