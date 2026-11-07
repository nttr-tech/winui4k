package com.appkitbox.winui4k.swing_to_winui4k.jtextfield.readonly;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** 2.7 Make a field read-only (Swing). */
public class ReadOnlySwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("2.7 Read Only (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JTextField id = new JTextField(10);
            id.setText("A-0001");
            id.setEditable(false);

            frame.add(id);

            frame.setSize(420, 140);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
