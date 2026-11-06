package com.appkitbox.winui4k.swing_to_winui4k.jtextfield;

import java.awt.FlowLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** 2.9 Selecting all when the focus arrives (Swing). */
public class SelectAllSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("2.9 Select all on focus (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JTextField amount = new JTextField(10);
            amount.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    amount.selectAll();
                }
            });
            amount.setText("12800");
            amount.selectAll();

            frame.add(amount);

            frame.setSize(420, 140);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
