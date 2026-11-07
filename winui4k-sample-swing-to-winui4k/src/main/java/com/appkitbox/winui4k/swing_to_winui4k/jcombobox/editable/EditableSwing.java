package com.appkitbox.winui4k.swing_to_winui4k.jcombobox.editable;

import java.awt.FlowLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/** 8.5 Let the user type a value (Swing). */
public class EditableSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("8.5 Editable (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JComboBox<String> font = new JComboBox<>(new String[] {"Arial", "Segoe UI", "Consolas"});
            font.setEditable(true);
            JLabel result = new JLabel("Font: " + font.getSelectedItem());
            font.addActionListener(e -> result.setText("Font: " + font.getSelectedItem()));

            frame.add(font);
            frame.add(result);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            font.setSelectedItem("Courier New");
        });
    }
}
