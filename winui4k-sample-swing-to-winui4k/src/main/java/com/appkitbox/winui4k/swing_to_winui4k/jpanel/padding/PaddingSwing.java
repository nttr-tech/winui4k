package com.appkitbox.winui4k.swing_to_winui4k.jpanel.padding;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** 14.5 Leave space inside a panel (Swing). */
public class PaddingSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("14.5 Padding (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JPanel content = new JPanel(new GridLayout(2, 2, 8, 8));
            content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
            content.setBackground(new Color(230, 240, 250));
            content.add(new JButton("One"));
            content.add(new JButton("Two"));
            content.add(new JButton("Three"));
            content.add(new JButton("Four"));

            frame.add(content, BorderLayout.CENTER);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
