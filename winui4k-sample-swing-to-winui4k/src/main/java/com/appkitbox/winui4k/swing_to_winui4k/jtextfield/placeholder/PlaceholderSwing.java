package com.appkitbox.winui4k.swing_to_winui4k.jtextfield.placeholder;

import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** 2.8 Add placeholder text and a header (Swing). */
public class PlaceholderSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("2.8 Placeholder and Header (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            // Swing has no placeholder feature. For a header, put a separate JLabel and tie it with setLabelFor
            JLabel label = new JLabel("Name");
            JTextField name = new JTextField(20);
            label.setLabelFor(name);
            label.setDisplayedMnemonic(KeyEvent.VK_N);
            frame.add(label);
            frame.add(name);
            // Draw a placeholder by overriding paintComponent, or rely on a library (e.g. FlatLaf's JTextField.placeholderText)

            frame.setSize(400, 180);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
