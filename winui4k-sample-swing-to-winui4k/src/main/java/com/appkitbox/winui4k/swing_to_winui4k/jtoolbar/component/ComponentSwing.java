package com.appkitbox.winui4k.swing_to_winui4k.jtoolbar.component;

import java.awt.BorderLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

/** 23.5 Put a combo box and a text field on the toolbar (Swing). */
public class ComponentSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("23.5 Toolbar Components (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            JLabel status = new JLabel("Font: Segoe UI");

            JComboBox<String> font = new JComboBox<>(new String[] {"Segoe UI", "Yu Gothic UI", "Consolas"});
            font.setMaximumSize(font.getPreferredSize());   // don't stretch to the toolbar width
            font.addActionListener(e -> status.setText("Font: " + font.getSelectedItem()));
            JTextField search = new JTextField(10);
            search.setMaximumSize(search.getPreferredSize());
            search.addActionListener(e -> status.setText("Search: " + search.getText()));

            JToolBar toolBar = new JToolBar();
            toolBar.add(new JLabel("Font: "));
            toolBar.add(font);
            toolBar.addSeparator();
            toolBar.add(search);

            frame.add(toolBar, BorderLayout.NORTH);
            frame.add(status, BorderLayout.CENTER);

            frame.setSize(460, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
