package com.appkitbox.winui4k.swing_to_winui4k.jspinner.listener;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

/** 9.3 Read the value and react to changes (Swing). */
public class ListenerSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("9.3 Value changes (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JSpinner age = new JSpinner(new SpinnerNumberModel(20, 0, 120, 1));
            JLabel result = new JLabel("Age: 20");
            age.addChangeListener(e -> {
                int value = (Integer) age.getValue();
                result.setText("Age: " + value);
            });

            frame.add(age);
            frame.add(result);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            age.setValue(35);
        });
    }
}
