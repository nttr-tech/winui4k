package com.appkitbox.winui4k.swing_to_winui4k.jtextfield;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** 2.3 Handling Enter (Swing). */
public class EnterSwing {
    static void search(String text) {
        System.out.println("Search: " + text);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("2.3 Handling Enter (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JTextField query = new JTextField(20);
            query.addActionListener(e -> search(query.getText()));
            query.setText("Apple");

            frame.add(query);

            frame.setSize(400, 140);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
