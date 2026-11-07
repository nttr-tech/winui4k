package com.appkitbox.winui4k.swing_to_winui4k.jpanel.border;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** 14.3 Draw a border around a panel (Swing). */
public class BorderSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("14.3 Border (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JPanel style = new JPanel(new GridLayout(0, 1));
            style.add(new JCheckBox("Bold"));
            style.add(new JCheckBox("Italic"));
            style.add(new JCheckBox("Underline"));
            style.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.GRAY, 1),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)));

            frame.add(style);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
