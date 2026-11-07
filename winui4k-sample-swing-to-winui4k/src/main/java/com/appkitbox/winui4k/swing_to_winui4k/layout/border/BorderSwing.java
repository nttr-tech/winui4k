package com.appkitbox.winui4k.swing_to_winui4k.layout.border;

import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** 30.3 BorderLayout (Swing). */
public class BorderSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("30.3 BorderLayout (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // the content pane of a JFrame uses BorderLayout by default
            frame.add(new JButton("North"), BorderLayout.NORTH);
            frame.add(new JButton("South"), BorderLayout.SOUTH);
            frame.add(new JButton("West"), BorderLayout.WEST);
            frame.add(new JButton("East"), BorderLayout.EAST);
            frame.add(new JButton("Center"));                    // no constraint = CENTER

            frame.setSize(400, 240);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
