package com.appkitbox.winui4k.swing_to_winui4k.jtogglebutton.basic;

import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

/** 7.2 Turn on and off (Swing). */
public class BasicSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("7.2 Turn on and off (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JLabel preview = new JLabel("Sample text");
            JToggleButton italic = new JToggleButton("Italic");
            italic.addItemListener(e -> preview.setFont(
                    preview.getFont().deriveFont(italic.isSelected() ? Font.ITALIC : Font.PLAIN)));

            frame.add(italic);
            frame.add(preview);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            italic.setSelected(true); // turn it on once at startup
        });
    }
}
