package com.appkitbox.winui4k.swing_to_winui4k.jbutton;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** 1.11 Fonts, colors and flat buttons (Swing). */
public class AppearanceSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("1.11 Appearance (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JButton danger = new JButton("Delete");
            danger.setFont(new Font("Yu Gothic UI", Font.BOLD, 14));
            danger.setForeground(Color.WHITE);
            danger.setBackground(new Color(196, 43, 28));

            JButton link = new JButton("Learn more");
            link.setContentAreaFilled(false);     // Do not fill the background
            link.setBorderPainted(false);         // Do not paint the border
            link.setFocusPainted(false);          // Do not paint the focus border

            frame.add(danger);
            frame.add(link);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
