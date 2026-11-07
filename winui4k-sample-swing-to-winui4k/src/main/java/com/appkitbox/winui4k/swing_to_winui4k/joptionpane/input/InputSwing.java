package com.appkitbox.winui4k.swing_to_winui4k.joptionpane.input;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** 26.4 Ask for a text (Swing). */
public class InputSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("26.4 Input (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());
            JLabel status = new JLabel("Waiting for the answer...");
            frame.add(status);
            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            String name = JOptionPane.showInputDialog(frame, "Your name:", "Alice");
            if (name != null) {                        // null when canceled
                status.setText("Hello, " + name);
            }
        });
    }
}
