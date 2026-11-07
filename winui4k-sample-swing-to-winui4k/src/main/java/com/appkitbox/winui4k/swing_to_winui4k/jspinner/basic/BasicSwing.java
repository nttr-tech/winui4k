package com.appkitbox.winui4k.swing_to_winui4k.jspinner.basic;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

/** 9.2 Number spinner (Swing). */
public class BasicSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("9.2 Number spinner (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 0, 10, 1));

            frame.add(new JLabel("Quantity:"));
            frame.add(quantity);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
