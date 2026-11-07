package com.appkitbox.winui4k.swing_to_winui4k.jradiobutton.group;

import java.awt.FlowLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

/** 6.2 Choose one with ButtonGroup (Swing). */
public class GroupSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("6.2 Choose one (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JRadioButton small = new JRadioButton("Small");
            JRadioButton medium = new JRadioButton("Medium", true);
            JRadioButton large = new JRadioButton("Large");
            ButtonGroup size = new ButtonGroup();
            size.add(small);
            size.add(medium);
            size.add(large);

            frame.add(small);
            frame.add(medium);
            frame.add(large);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
