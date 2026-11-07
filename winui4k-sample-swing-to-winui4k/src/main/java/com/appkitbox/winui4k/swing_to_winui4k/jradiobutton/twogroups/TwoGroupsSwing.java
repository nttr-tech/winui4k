package com.appkitbox.winui4k.swing_to_winui4k.jradiobutton.twogroups;

import java.awt.FlowLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

/** 6.6 Two groups in the same panel (Swing). */
public class TwoGroupsSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("6.6 Two groups (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JRadioButton small = new JRadioButton("Small", true);
            JRadioButton large = new JRadioButton("Large");
            ButtonGroup size = new ButtonGroup();
            size.add(small);
            size.add(large);

            JRadioButton red = new JRadioButton("Red", true);
            JRadioButton blue = new JRadioButton("Blue");
            ButtonGroup color = new ButtonGroup();
            color.add(red);
            color.add(blue);

            frame.add(small);
            frame.add(large);
            frame.add(red);
            frame.add(blue);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            blue.setSelected(true); // changes only the color group
        });
    }
}
