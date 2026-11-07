package com.appkitbox.winui4k.swing_to_winui4k.jcombobox.basic;

import java.awt.FlowLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/** 8.2 Choose from fixed items (Swing). */
public class BasicSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("8.2 Fixed items (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JComboBox<String> size = new JComboBox<>(new String[] {"Small", "Medium", "Large"});
            size.setSelectedIndex(1);
            JLabel result = new JLabel("Selected: " + size.getSelectedItem());

            frame.add(size);
            frame.add(result);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
