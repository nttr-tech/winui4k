package com.appkitbox.winui4k.swing_to_winui4k.jbutton;

import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** 1.3 Switching between enabled and disabled (Swing). */
public class EnabledSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("1.3 Enabled and disabled (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JCheckBox agree = new JCheckBox("I agree to the terms of use");
            JButton next = new JButton("Next");
            next.setEnabled(false);
            agree.addItemListener(e -> next.setEnabled(agree.isSelected()));

            frame.add(agree);
            frame.add(next);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
