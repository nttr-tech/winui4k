package com.appkitbox.winui4k.swing_to_winui4k.layout.grid;

import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** 30.4 GridLayout (Swing). */
public class GridSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("30.4 GridLayout (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            frame.setLayout(new GridLayout(0, 3, 5, 5));         // any number of rows, 3 columns
            for (String key : new String[] {"7", "8", "9", "4", "5", "6", "1", "2", "3", "0", ".", "="}) {
                frame.add(new JButton(key));
            }

            frame.setSize(260, 240);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
