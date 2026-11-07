package com.appkitbox.winui4k.swing_to_winui4k.jspinner.range;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

/** 9.5 Change the range from another value (Swing). */
public class RangeSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("9.5 Linked range (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            SpinnerNumberModel fromModel = new SpinnerNumberModel(1, 1, 31, 1);
            SpinnerNumberModel toModel = new SpinnerNumberModel(5, 1, 31, 1);
            fromModel.addChangeListener(e -> toModel.setMinimum((Integer) fromModel.getValue()));

            frame.add(new JLabel("From:"));
            frame.add(new JSpinner(fromModel));
            frame.add(new JLabel("To:"));
            frame.add(new JSpinner(toModel));

            frame.setSize(520, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            fromModel.setValue(10);
        });
    }
}
