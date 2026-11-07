package com.appkitbox.winui4k.swing_to_winui4k.jpanel.background;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** 14.4 Paint the background of a panel (Swing). */
public class BackgroundSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("14.4 Background (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
            header.setBackground(new Color(0, 90, 158));
            JLabel title = new JLabel("Settings");
            title.setForeground(Color.WHITE);
            header.add(title);

            frame.add(header, BorderLayout.NORTH);
            frame.add(new JLabel("Content"), BorderLayout.CENTER);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
