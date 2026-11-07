package com.appkitbox.winui4k.swing_to_winui4k.jlabel.font;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/** 4.3 Font and color (Swing). */
public class FontSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("4.3 Font and color (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JLabel heading = new JLabel("Monthly report");
            heading.setFont(heading.getFont().deriveFont(Font.BOLD, 20f));
            JLabel error = new JLabel("The file could not be saved.");
            error.setForeground(Color.RED);
            JLabel note = new JLabel("Last updated: 2026-11-07");
            note.setFont(new Font("SansSerif", Font.ITALIC, 12));

            frame.add(heading);
            frame.add(error);
            frame.add(note);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
