package com.appkitbox.winui4k.swing_to_winui4k.jtogglebutton.group;

import java.awt.FlowLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

/** 7.4 Press only one with ButtonGroup (Swing). */
public class GroupSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("7.4 Press only one (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JToggleButton left = new JToggleButton("Left", true);
            JToggleButton center = new JToggleButton("Center");
            JToggleButton right = new JToggleButton("Right");
            ButtonGroup alignment = new ButtonGroup();
            alignment.add(left);
            alignment.add(center);
            alignment.add(right);

            frame.add(left);
            frame.add(center);
            frame.add(right);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            center.doClick(); // pressing "Center" releases "Left"
        });
    }
}
