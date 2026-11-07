package com.appkitbox.winui4k.swing_to_winui4k.jtextarea.appearance;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/** 12.6 Read-only text with a monospaced font (Swing). */
public class AppearanceSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("12.6 Read-only and font (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JTextArea license = new JTextArea(6, 50);
            license.setText("MIT License\n\nPermission is hereby granted, free of charge,\n"
                    + "to any person obtaining a copy of this software.");
            license.setEditable(false);
            license.setFont(new Font("Consolas", Font.PLAIN, 14));
            license.setForeground(new Color(0, 90, 158));

            frame.add(new JScrollPane(license));

            frame.setSize(640, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
