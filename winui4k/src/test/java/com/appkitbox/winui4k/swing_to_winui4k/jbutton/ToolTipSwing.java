package com.appkitbox.winui4k.swing_to_winui4k.jbutton;

import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** 1.5 Showing a tooltip (Swing). */
public class ToolTipSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("1.5 Tooltip (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JButton refresh = new JButton("Refresh");
            refresh.setToolTipText("Brings the list up to date (F5)");

            frame.add(refresh);

            frame.setSize(400, 150);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
