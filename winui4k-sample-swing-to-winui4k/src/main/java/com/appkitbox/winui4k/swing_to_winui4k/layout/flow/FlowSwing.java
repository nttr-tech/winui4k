package com.appkitbox.winui4k.swing_to_winui4k.layout.flow;

import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** 30.2 FlowLayout (Swing). */
public class FlowSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("30.2 FlowLayout (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            frame.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
            for (String name : new String[] {"One", "Two", "Three", "Four", "Five", "Six"}) {
                frame.add(new JButton(name));
            }

            frame.setSize(320, 180);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
