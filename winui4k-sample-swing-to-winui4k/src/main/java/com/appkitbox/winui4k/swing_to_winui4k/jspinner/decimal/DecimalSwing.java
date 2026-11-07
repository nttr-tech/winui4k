package com.appkitbox.winui4k.swing_to_winui4k.jspinner.decimal;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

/** 9.4 Decimal steps (Swing). */
public class DecimalSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("9.4 Decimal steps (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JSpinner price = new JSpinner(new SpinnerNumberModel(12.0, 0.0, 100.0, 0.5));
            price.setEditor(new JSpinner.NumberEditor(price, "0.00"));

            frame.add(new JLabel("Price:"));
            frame.add(price);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
